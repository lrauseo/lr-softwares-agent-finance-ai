package com.lrsoftwares.finance_ai_agent.service.sprint8;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.dto.CreateTransactionRequest;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.CsvColumnMapping;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.ImportTransactionsResponse;
import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;
import com.lrsoftwares.finance_ai_agent.repository.CategoryRepository;
import com.lrsoftwares.finance_ai_agent.service.TransactionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionImportService {

    private static final Pattern OFX_TAG_PATTERN = Pattern.compile("<([A-Z0-9]+)>([^<\\r\\n]+)", Pattern.CASE_INSENSITIVE);

    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final CategoryRepository categoryRepository;
    private final TransactionService transactionService;
    private final ExpenseAutoClassificationService expenseAutoClassificationService;

    public ImportTransactionsResponse importCsv(MultipartFile file) {
        return importCsv(file, null);
    }

    public ImportTransactionsResponse importCsv(MultipartFile file, CsvColumnMapping columnMapping) {
        List<String> warnings = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String[] lines = content.split("\\R", 2);
            if (lines.length == 0 || lines[0].isBlank()) {
                return new ImportTransactionsResponse(0, 0, List.of("Arquivo CSV sem linhas de dados."));
            }

            char delimiter = detectDelimiter(lines[0]);
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimiter)
                    .setQuote('"')
                    .setEscape('\\')
                    .setIgnoreEmptyLines(true)
                    .setTrim(false)
                    .build();

            ColumnIndices columnIndices = null;

            try (CSVParser parser = CSVParser.parse(new StringReader(content), format)) {
                for (CSVRecord record : parser) {
                    if (isBlankRecord(record)) {
                        continue;
                    }
                    if (record.getRecordNumber() == 1
                            && (looksLikeHeader(record) || mappingRequiresHeader(columnMapping))) {
                        try {
                            Map<String, Integer> headerMap = buildHeaderMap(record);
                            columnIndices = resolveColumnIndices(columnMapping, headerMap);
                        } catch (IllegalArgumentException ex) {
                            return new ImportTransactionsResponse(0, 0,
                                    List.of("Mapeamento de colunas invalido: " + ex.getMessage()));
                        }
                        continue;
                    }

                    if (columnIndices == null) {
                        try {
                            columnIndices = resolveColumnIndices(columnMapping, Map.of());
                        } catch (IllegalArgumentException ex) {
                            return new ImportTransactionsResponse(0, 0,
                                    List.of("Mapeamento de colunas invalido: " + ex.getMessage()));
                        }
                    }

                    try {
                        ImportedRow row = parseCsvRecord(record, columnIndices);
                        saveImportedRow(row);
                        imported++;
                    } catch (Exception ex) {
                        skipped++;
                        warnings.add("Registro " + record.getRecordNumber() + " ignorado: " + ex.getMessage());
                    }
                }
            }

            if (imported == 0 && skipped == 0) {
                return new ImportTransactionsResponse(0, 0, List.of("Arquivo CSV sem linhas de dados."));
            }

            return new ImportTransactionsResponse(imported, skipped, warnings);
        } catch (IOException ex) {
            return new ImportTransactionsResponse(0, 1, List.of("Falha ao ler arquivo CSV: " + ex.getMessage()));
        }
    }

    private Map<String, Integer> buildHeaderMap(CSVRecord headerRecord) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < headerRecord.size(); i++) {
            String name = headerRecord.get(i).trim().toLowerCase(Locale.ROOT);
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Cabecalho vazio encontrado na coluna " + i);
            }
            if (map.containsKey(name)) {
                throw new IllegalArgumentException("Nome de coluna duplicado no cabecalho: '" + name + "'");
            }
            map.put(name, i);
        }
        return map;
    }

    private ColumnIndices resolveColumnIndices(CsvColumnMapping mapping, Map<String, Integer> headerMap) {
        if (mapping == null) {
            return new ColumnIndices(0, 1, 2, 3, 4, 5);
        }
        return new ColumnIndices(
                resolveIndex(mapping.date(), headerMap, "date", 0),
                resolveIndex(mapping.description(), headerMap, "description", 1),
                resolveIndex(mapping.amount(), headerMap, "amount", 2),
                resolveIndex(mapping.type(), headerMap, "type", 3),
                resolveIndex(mapping.category(), headerMap, "category", 4),
                resolveIndex(mapping.recurring(), headerMap, "recurring", 5));
    }

    private int resolveIndex(String spec, Map<String, Integer> headerMap, String fieldName, int defaultIndex) {
        if (spec == null || spec.isBlank()) {
            return defaultIndex;
        }
        try {
            int index = Integer.parseInt(spec.trim());
            if (index < 0) {
                throw new IllegalArgumentException(
                        "Indice de coluna invalido para o campo '" + fieldName + "': " + spec + ". O indice deve ser >= 0.");
            }
            return index;
        } catch (NumberFormatException e) {
            Integer idx = headerMap.get(spec.trim().toLowerCase(Locale.ROOT));
            if (idx != null) {
                return idx;
            }
            throw new IllegalArgumentException("Coluna nao encontrada para o campo '" + fieldName + "': " + spec);
        }
    }

    private ImportedRow parseCsvRecord(CSVRecord record, ColumnIndices cols) {
        int minRequired = Math.max(cols.date(), Math.max(cols.description(), cols.amount())) + 1;
        if (record.size() < minRequired) {
            throw new IllegalArgumentException("colunas insuficientes. Esperado: date,description,amount,...");
        }

        String dateValue = valueAt(record, cols.date());
        String description = valueAt(record, cols.description());
        String amountValue = valueAt(record, cols.amount());

        if (dateValue == null || description == null || amountValue == null) {
            throw new IllegalArgumentException("colunas obrigatorias ausentes (date, description, amount)");
        }

        LocalDate date = parseDate(dateValue);
        BigDecimal rawAmount = parseAmount(amountValue);

        String typeValue = valueAt(record, cols.type());
        TransactionType type = (typeValue != null && !typeValue.isBlank())
                ? parseType(typeValue, rawAmount)
                : (rawAmount.signum() < 0 ? TransactionType.EXPENSE : TransactionType.INCOME);

        String categoryName = valueAt(record, cols.category());
        String recurringValue = valueAt(record, cols.recurring());
        boolean recurring = recurringValue != null && Boolean.parseBoolean(recurringValue.trim());

        return new ImportedRow(date, description, rawAmount.abs(), type, categoryName, recurring);
    }

    private char detectDelimiter(String headerLine) {
        return headerLine.contains(";") ? ';' : ',';
    }

    private boolean mappingRequiresHeader(CsvColumnMapping mapping) {
        if (mapping == null) {
            return false;
        }
        return isHeaderName(mapping.date()) || isHeaderName(mapping.description())
                || isHeaderName(mapping.amount()) || isHeaderName(mapping.type())
                || isHeaderName(mapping.category()) || isHeaderName(mapping.recurring());
    }

    private boolean isHeaderName(String spec) {
        if (spec == null || spec.isBlank()) {
            return false;
        }
        try {
            Integer.parseInt(spec.trim());
            return false;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private boolean isBlankRecord(CSVRecord record) {
        for (int i = 0; i < record.size(); i++) {
            if (!record.get(i).trim().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean looksLikeHeader(CSVRecord record) {
        String first = valueAt(record, 0);
        String second = valueAt(record, 1);
        String third = valueAt(record, 2);

        if (first == null || second == null || third == null) {
            return false;
        }

        String f = first.trim().toLowerCase(Locale.ROOT);
        String s = second.trim().toLowerCase(Locale.ROOT);
        String t = third.trim().toLowerCase(Locale.ROOT);

        return (f.equals("date") || f.equals("data"))
                && (s.equals("description") || s.equals("descricao"))
                && (t.equals("amount") || t.equals("valor"));
    }

    private String valueAt(CSVRecord record, int index) {
        if (index >= record.size()) {
            return null;
        }
        String value = record.get(index);
        return value == null ? null : value.trim();
    }

    public ImportTransactionsResponse importOfx(MultipartFile file) {
        List<String> warnings = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String[] blocks = content.split("<STMTTRN>");

            for (int i = 1; i < blocks.length; i++) {
                String block = blocks[i];
                try {
                    ImportedRow row = parseOfxBlock(block);
                    saveImportedRow(row);
                    imported++;
                } catch (Exception ex) {
                    skipped++;
                    warnings.add("Transacao OFX " + i + " ignorada: " + ex.getMessage());
                }
            }

            return new ImportTransactionsResponse(imported, skipped, warnings);
        } catch (IOException ex) {
            return new ImportTransactionsResponse(0, 1, List.of("Falha ao ler arquivo OFX: " + ex.getMessage()));
        }
    }

    private ImportedRow parseOfxBlock(String block) {
        String dateTag = extractTag(block, "DTPOSTED");
        String amountTag = extractTag(block, "TRNAMT");
        String memo = extractTag(block, "MEMO");
        String name = extractTag(block, "NAME");
        String typeTag = extractTag(block, "TRNTYPE");

        LocalDate date = parseOfxDate(dateTag);
        BigDecimal rawAmount = parseAmount(amountTag);
        TransactionType type = parseType(typeTag, rawAmount);

        String description = (Objects.toString(name, "") + " " + Objects.toString(memo, "")).trim();
        if (description.isBlank()) {
            description = "Importacao OFX";
        }

        return new ImportedRow(date, description, rawAmount.abs(), type, null, false);
    }

    private void saveImportedRow(ImportedRow row) {
        UUID userId = authenticatedUserProvider.getUserId();
        Category category = resolveCategory(userId, row.categoryName(), row.type(), row.description());

        CreateTransactionRequest request = new CreateTransactionRequest(
                userId,
                category.getId(),
                row.date(),
                row.amount(),
                row.type(),
                row.description(),
                row.recurring());

        transactionService.salvar(request);
    }

    private Category resolveCategory(UUID userId, String categoryName, TransactionType type, String description) {
        if (categoryName != null && !categoryName.isBlank()) {
            var found = categoryRepository.findByUserIdAndNameIgnoreCaseAndType(userId, categoryName.trim(), type);
            if (found.isPresent()) {
                return found.get();
            }
        }

        if (type == TransactionType.EXPENSE) {
            var classified = expenseAutoClassificationService.classify(userId, description);
            return categoryRepository.findById(classified.categoryId())
                    .orElseGet(() -> createFallbackCategory(userId, type));
        }

        return createFallbackCategory(userId, type);
    }

    private Category createFallbackCategory(UUID userId, TransactionType type) {
        String defaultName = type == TransactionType.EXPENSE ? "Outras despesas" : "Outras receitas";

        return categoryRepository.findByUserIdAndNameIgnoreCaseAndType(userId, defaultName, type)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .userId(userId)
                        .name(defaultName)
                        .type(type)
                        .systemDefault(Boolean.FALSE)
                        .build()));
    }

    private LocalDate parseDate(String value) {
        try {
            if (value.contains("/")) {
                return LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("data invalida: " + value);
        }
    }

    private LocalDate parseOfxDate(String dateTag) {
        if (dateTag == null || dateTag.length() < 8) {
            throw new IllegalArgumentException("DTPOSTED invalido");
        }
        String normalized = dateTag.substring(0, 8);
        return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private BigDecimal parseAmount(String value) {
        String normalized = value.replace(" ", "").replace("+", "").replace(",", ".");
        return new BigDecimal(normalized);
    }

    private TransactionType parseType(String value, BigDecimal amount) {
        if (value == null || value.isBlank()) {
            return amount.signum() < 0 ? TransactionType.EXPENSE : TransactionType.INCOME;
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.contains("debit") || normalized.contains("payment") || normalized.contains("expense")
                || normalized.contains("saque")) {
            return TransactionType.EXPENSE;
        }

        if (normalized.contains("credit") || normalized.contains("deposit") || normalized.contains("income")) {
            return TransactionType.INCOME;
        }

        return amount.signum() < 0 ? TransactionType.EXPENSE : TransactionType.INCOME;
    }

    private String extractTag(String block, String tag) {
        Matcher matcher = OFX_TAG_PATTERN.matcher(block);
        while (matcher.find()) {
            if (tag.equalsIgnoreCase(matcher.group(1))) {
                return matcher.group(2).trim();
            }
        }
        return null;
    }

    private record ColumnIndices(int date, int description, int amount, int type, int category, int recurring) {
    }

    private record ImportedRow(
            LocalDate date,
            String description,
            BigDecimal amount,
            TransactionType type,
            String categoryName,
            boolean recurring) {
    }
}
