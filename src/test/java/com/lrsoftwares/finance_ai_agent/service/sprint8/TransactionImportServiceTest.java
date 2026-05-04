package com.lrsoftwares.finance_ai_agent.service.sprint8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.dto.CreateTransactionRequest;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.CsvColumnMapping;
import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;
import com.lrsoftwares.finance_ai_agent.repository.CategoryRepository;
import com.lrsoftwares.finance_ai_agent.service.TransactionService;

@ExtendWith(MockitoExtension.class)
class TransactionImportServiceTest {

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private ExpenseAutoClassificationService expenseAutoClassificationService;

    @InjectMocks
    private TransactionImportService service;

    @Test
    void shouldParseQuotedCsvWithDelimiterInsideDescription() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Category category = Category.builder()
                .id(categoryId)
                .userId(userId)
                .name("Alimentacao")
                .type(TransactionType.EXPENSE)
                .systemDefault(false)
                .build();

        String csv = "date;description;amount;type;category;recurring\n"
                + "2026-04-12;\"Mercado, atacado\";120.50;EXPENSE;Alimentacao;false\n";

        MockMultipartFile file = new MockMultipartFile("file", "dados.csv", "text/csv", csv.getBytes());

        when(authenticatedUserProvider.getUserId()).thenReturn(userId);
        when(categoryRepository.findByUserIdAndNameIgnoreCaseAndType(userId, "Alimentacao", TransactionType.EXPENSE))
                .thenReturn(Optional.of(category));

        var response = service.importCsv(file);

        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(0);

        ArgumentCaptor<CreateTransactionRequest> captor = ArgumentCaptor.forClass(CreateTransactionRequest.class);
        org.mockito.Mockito.verify(transactionService).salvar(captor.capture());

        CreateTransactionRequest captured = captor.getValue();
        assertThat(captured.description()).isEqualTo("Mercado, atacado");
        assertThat(captured.categoryId()).isEqualTo(categoryId);
        assertThat(captured.type()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void shouldSkipBrokenCsvRecordAndCollectWarning() {
        String csv = "date;description;amount\n2026-04-10;Compra;abc\n";
        MockMultipartFile file = new MockMultipartFile("file", "dados.csv", "text/csv", csv.getBytes());

        var response = service.importCsv(file);

        assertThat(response.importedCount()).isEqualTo(0);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.warnings()).isNotEmpty();
        org.mockito.Mockito.verify(transactionService, org.mockito.Mockito.never()).salvar(any());
    }

    @Test
    void shouldUseMappingByColumnIndex() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Category category = Category.builder()
                .id(categoryId)
                .userId(userId)
                .name("Salario")
                .type(TransactionType.INCOME)
                .systemDefault(false)
                .build();

        // CSV has columns in non-standard order: amount;type;date;description;category;recurring
        String csv = "250.00;INCOME;2026-05-01;Salario mensal;Salario;false\n";

        MockMultipartFile file = new MockMultipartFile("file", "dados.csv", "text/csv", csv.getBytes());

        CsvColumnMapping mapping = new CsvColumnMapping("2", "3", "0", "1", "4", "5");

        when(authenticatedUserProvider.getUserId()).thenReturn(userId);
        when(categoryRepository.findByUserIdAndNameIgnoreCaseAndType(userId, "Salario", TransactionType.INCOME))
                .thenReturn(Optional.of(category));

        var response = service.importCsv(file, mapping);

        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(0);

        ArgumentCaptor<CreateTransactionRequest> captor = ArgumentCaptor.forClass(CreateTransactionRequest.class);
        org.mockito.Mockito.verify(transactionService).salvar(captor.capture());

        CreateTransactionRequest captured = captor.getValue();
        assertThat(captured.description()).isEqualTo("Salario mensal");
        assertThat(captured.type()).isEqualTo(TransactionType.INCOME);
    }

    @Test
    void shouldUseMappingByHeaderName() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Category category = Category.builder()
                .id(categoryId)
                .userId(userId)
                .name("Alimentacao")
                .type(TransactionType.EXPENSE)
                .systemDefault(false)
                .build();

        // CSV has columns with custom header names
        String csv = "valor;tipo;dt;descricao;categoria;recorrente\n"
                + "80.00;EXPENSE;2026-05-02;Supermercado;Alimentacao;false\n";

        MockMultipartFile file = new MockMultipartFile("file", "dados.csv", "text/csv", csv.getBytes());

        CsvColumnMapping mapping = new CsvColumnMapping("dt", "descricao", "valor", "tipo", "categoria", "recorrente");

        when(authenticatedUserProvider.getUserId()).thenReturn(userId);
        when(categoryRepository.findByUserIdAndNameIgnoreCaseAndType(userId, "Alimentacao", TransactionType.EXPENSE))
                .thenReturn(Optional.of(category));

        var response = service.importCsv(file, mapping);

        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(0);

        ArgumentCaptor<CreateTransactionRequest> captor = ArgumentCaptor.forClass(CreateTransactionRequest.class);
        org.mockito.Mockito.verify(transactionService).salvar(captor.capture());

        CreateTransactionRequest captured = captor.getValue();
        assertThat(captured.description()).isEqualTo("Supermercado");
        assertThat(captured.type()).isEqualTo(TransactionType.EXPENSE);
    }

    @Test
    void shouldUseDefaultMappingWhenNullProvided() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        Category category = Category.builder()
                .id(categoryId)
                .userId(userId)
                .name("Alimentacao")
                .type(TransactionType.EXPENSE)
                .systemDefault(false)
                .build();

        String csv = "date;description;amount;type;category;recurring\n"
                + "2026-04-12;Padaria;30.00;EXPENSE;Alimentacao;false\n";

        MockMultipartFile file = new MockMultipartFile("file", "dados.csv", "text/csv", csv.getBytes());

        when(authenticatedUserProvider.getUserId()).thenReturn(userId);
        when(categoryRepository.findByUserIdAndNameIgnoreCaseAndType(userId, "Alimentacao", TransactionType.EXPENSE))
                .thenReturn(Optional.of(category));

        var response = service.importCsv(file, null);

        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.skippedCount()).isEqualTo(0);
    }

    @Test
    void shouldReturnErrorResponseWhenMappingReferencesNonexistentHeader() {
        String csv = "date;description;amount\n2026-05-01;Salario;1000.00\n";
        MockMultipartFile file = new MockMultipartFile("file", "dados.csv", "text/csv", csv.getBytes());

        CsvColumnMapping mapping = new CsvColumnMapping("nonexistent", "description", "amount", null, null, null);

        var response = service.importCsv(file, mapping);

        assertThat(response.importedCount()).isEqualTo(0);
        assertThat(response.skippedCount()).isEqualTo(0);
        assertThat(response.warnings()).hasSize(1);
        assertThat(response.warnings().get(0)).contains("Mapeamento de colunas invalido");
        org.mockito.Mockito.verify(transactionService, org.mockito.Mockito.never()).salvar(any());
    }

    @Test
    void shouldReturnErrorResponseWhenMappingHasNegativeIndex() {
        String csv = "2026-05-01;Salario;1000.00\n";
        MockMultipartFile file = new MockMultipartFile("file", "dados.csv", "text/csv", csv.getBytes());

        CsvColumnMapping mapping = new CsvColumnMapping("-1", "1", "2", null, null, null);

        var response = service.importCsv(file, mapping);

        assertThat(response.importedCount()).isEqualTo(0);
        assertThat(response.skippedCount()).isEqualTo(0);
        assertThat(response.warnings()).hasSize(1);
        assertThat(response.warnings().get(0)).contains("Mapeamento de colunas invalido");
        org.mockito.Mockito.verify(transactionService, org.mockito.Mockito.never()).salvar(any());
    }

    @Test
    void shouldReturnErrorResponseWhenHeaderHasDuplicateNames() {
        // CSV with duplicate header column name
        String csv = "date;description;date\n2026-05-01;Salario;1000.00\n";
        MockMultipartFile file = new MockMultipartFile("file", "dados.csv", "text/csv", csv.getBytes());

        // Mapping by header name forces header parsing
        CsvColumnMapping mapping = new CsvColumnMapping("date", "description", "amount", null, null, null);

        var response = service.importCsv(file, mapping);

        assertThat(response.importedCount()).isEqualTo(0);
        assertThat(response.skippedCount()).isEqualTo(0);
        assertThat(response.warnings()).hasSize(1);
        assertThat(response.warnings().get(0)).contains("duplicado");
        org.mockito.Mockito.verify(transactionService, org.mockito.Mockito.never()).salvar(any());
    }

    @Test
    void shouldSkipRecordWhenMappedRequiredColumnIsOutOfRange() {
        // Mapping puts 'date' at index 5, but record only has 3 columns → insufficient columns
        String csv = "1000.00;Salario;2026-05-01\n";
        MockMultipartFile file = new MockMultipartFile("file", "dados.csv", "text/csv", csv.getBytes());

        CsvColumnMapping mapping = new CsvColumnMapping("5", "1", "0", null, null, null);

        var response = service.importCsv(file, mapping);

        assertThat(response.importedCount()).isEqualTo(0);
        assertThat(response.skippedCount()).isEqualTo(1);
        assertThat(response.warnings()).isNotEmpty();
        org.mockito.Mockito.verify(transactionService, org.mockito.Mockito.never()).salvar(any());
    }
}

