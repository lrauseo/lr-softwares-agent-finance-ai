package com.lrsoftwares.finance_ai_agent.service.sprint8;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.dto.CreateTransactionRequest;
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
        List<String> warnings = new ArrayList<>();
        int imported = 0;
        int skipped = 0;

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String[] lines = content.split("\\R");
            if (lines.length <= 1) {
                return new ImportTransactionsResponse(0, 0, List.of("Arquivo CSV sem linhas de dados."));
            }

            String delimiter = lines[0].contains(";") ? ";" : ",";

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isBlank()) {
                    continue;
                }

                try {
                    ImportedRow row = parseCsvLine(line, delimiter);
                    saveImportedRow(row);
                    imported++;
                } catch (Exception ex) {
                    skipped++;
                    warnings.add("Linha " + (i + 1) + " ignorada: " + ex.getMessage());
                }
            }

            return new ImportTransactionsResponse(imported, skipped, warnings);
        } catch (IOException ex) {
            return new ImportTransactionsResponse(0, 1, List.of("Falha ao ler arquivo CSV: " + ex.getMessage()));
        }
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

    private ImportedRow parseCsvLine(String line, String delimiter) {
        String[] cols = line.split(Pattern.quote(delimiter), -1);
        if (cols.length < 3) {
            throw new IllegalArgumentException("colunas insuficientes. Esperado: date,description,amount,...");
        }

        LocalDate date = parseDate(cols[0].trim());
        String description = cols[1].trim();
        BigDecimal rawAmount = parseAmount(cols[2].trim());

        TransactionType type = cols.length > 3 && !cols[3].isBlank()
                ? parseType(cols[3].trim(), rawAmount)
                : (rawAmount.signum() < 0 ? TransactionType.EXPENSE : TransactionType.INCOME);

        String categoryName = cols.length > 4 ? cols[4].trim() : null;
        boolean recurring = cols.length > 5 && Boolean.parseBoolean(cols[5].trim());

        return new ImportedRow(date, description, rawAmount.abs(), type, categoryName, recurring);
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

    private record ImportedRow(
            LocalDate date,
            String description,
            BigDecimal amount,
            TransactionType type,
            String categoryName,
            boolean recurring) {
    }
}
