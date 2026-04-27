package com.lrsoftwares.finance_ai_agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.dto.TransactionResponse;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;

@ExtendWith(MockitoExtension.class)
class SummaryServiceTest {

    @Mock
    private TransactionService transactionService;

        @Mock
        private AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    private SummaryService summaryService;

    @Test
    void getMonthlySummaryShouldCalculateIncomeExpenseBalanceAndCategoryTotals() {
        UUID userId = UUID.randomUUID();
        LocalDate monthDate = LocalDate.of(2026, 4, 15);

        UUID salaryCategoryId = UUID.randomUUID();
        UUID housingCategoryId = UUID.randomUUID();
        UUID leisureCategoryId = UUID.randomUUID();

        List<TransactionResponse> transactions = List.of(
                new TransactionResponse(UUID.randomUUID(), userId, salaryCategoryId, "Salario", LocalDate.of(2026, 4, 1),
                        new BigDecimal("3000.00"), TransactionType.INCOME, "Renda principal", false),
                new TransactionResponse(UUID.randomUUID(), userId, salaryCategoryId, "Salario", LocalDate.of(2026, 4, 20),
                        new BigDecimal("500.00"), TransactionType.INCOME, "Renda extra", false),
                new TransactionResponse(UUID.randomUUID(), userId, housingCategoryId, "Moradia", LocalDate.of(2026, 4, 5),
                        new BigDecimal("1200.00"), TransactionType.EXPENSE, "Aluguel", true),
                new TransactionResponse(UUID.randomUUID(), userId, leisureCategoryId, "Lazer", LocalDate.of(2026, 4, 12),
                        new BigDecimal("300.00"), TransactionType.EXPENSE, "Cinema", false));

        when(authenticatedUserProvider.getUserId()).thenReturn(userId);
        when(transactionService.getByUserAndDate(userId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .thenReturn(transactions);

        var result = summaryService.getMonthlySummary(monthDate);

        assertThat(result.totalIncome()).isEqualByComparingTo("3500.00");
        assertThat(result.totalExpense()).isEqualByComparingTo("1500.00");
        assertThat(result.balance()).isEqualByComparingTo("2000.00");

        Map<String, BigDecimal> categoryTotals = result.categories().stream()
                .collect(Collectors.toMap(category -> category.category(), category -> category.total()));

        assertThat(categoryTotals).containsEntry("Moradia", new BigDecimal("1200.00"));
        assertThat(categoryTotals).containsEntry("Lazer", new BigDecimal("300.00"));
    }

    @Test
    void getSummaryMonthlyByUserIdAndDateShouldUseYearMonthBoundaries() {
        UUID userId = UUID.randomUUID();
        YearMonth month = YearMonth.of(2026, 2);

        when(authenticatedUserProvider.getUserId()).thenReturn(userId);
        when(transactionService.getByUserAndDate(userId, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
                .thenReturn(List.of());

        var result = summaryService.getSummaryMonthlyByUserIdAndDate(month);

        verify(transactionService).getByUserAndDate(userId, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));
        assertThat(result.totalIncome()).isEqualByComparingTo("0");
        assertThat(result.totalExpense()).isEqualByComparingTo("0");
        assertThat(result.balance()).isEqualByComparingTo("0");
        assertThat(result.categories()).isEmpty();
    }
}
