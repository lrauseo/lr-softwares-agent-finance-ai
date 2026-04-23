package com.lrsoftwares.finance_ai_agent.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lrsoftwares.finance_ai_agent.dto.CategoryTotalResponse;
import com.lrsoftwares.finance_ai_agent.dto.FinancialAlert;
import com.lrsoftwares.finance_ai_agent.dto.MonthlySummaryResponse;
import com.lrsoftwares.finance_ai_agent.dto.TransactionResponse;
import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;
import com.lrsoftwares.finance_ai_agent.service.SummaryService;
import com.lrsoftwares.finance_ai_agent.service.TransactionService;

@ExtendWith(MockitoExtension.class)
class FinancialAnalysisServiceImplTest {

    @Mock
    private SummaryService summaryService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private FinancialAnalysisServiceImpl financialAnalysisService;

    @Test
    void analyzeMonthlyHealthShouldReturnAlertsForAllConfiguredRules() {
        UUID userId = UUID.randomUUID();
        YearMonth month = YearMonth.of(2026, 4);

        MonthlySummaryResponse currentSummary = new MonthlySummaryResponse(
                new BigDecimal("1000.00"),
                new BigDecimal("-1100.00"),
                new BigDecimal("-100.00"),
                List.of(
                        new CategoryTotalResponse("Moradia", new BigDecimal("400.00")),
                        new CategoryTotalResponse("Lazer", new BigDecimal("210.00"))));

        MonthlySummaryResponse previousSummary = new MonthlySummaryResponse(
                new BigDecimal("1000.00"),
                new BigDecimal("-700.00"),
                new BigDecimal("300.00"),
                List.of(
                        new CategoryTotalResponse("Moradia", new BigDecimal("200.00")),
                        new CategoryTotalResponse("Lazer", new BigDecimal("200.00"))));

        Category subscriptions = Category.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Assinaturas")
                .type(TransactionType.EXPENSE)
                .systemDefault(false)
                .build();

        when(summaryService.getSummaryMonthlyByUserIdAndDate(userId, month)).thenReturn(currentSummary);
        when(summaryService.getSummaryMonthlyByUserIdAndDate(userId, month.minusMonths(1))).thenReturn(previousSummary);
        when(transactionService.getByUserAndDate(userId, month.atDay(1), month.atEndOfMonth()))
                .thenReturn(List.of(
                        new TransactionResponse(UUID.randomUUID(), userId, subscriptions, LocalDate.of(2026, 4, 3),
                                new BigDecimal("-250.00"), TransactionType.EXPENSE, "Streaming", true),
                        new TransactionResponse(UUID.randomUUID(), userId, subscriptions, LocalDate.of(2026, 4, 8),
                                new BigDecimal("-50.00"), TransactionType.EXPENSE, "App", false),
                        new TransactionResponse(UUID.randomUUID(), userId, subscriptions, LocalDate.of(2026, 4, 10),
                                new BigDecimal("100.00"), TransactionType.INCOME, "Reembolso", true)));

        List<FinancialAlert> alerts = financialAnalysisService.analyzeMonthlyHealth(userId, month);

        assertThat(alerts).hasSize(5);
        assertThat(alerts).extracting(FinancialAlert::code)
                .containsExactlyInAnyOrder("FA-001", "FA-002", "FA-003", "FA-004", "FA-005");
        assertThat(alerts).filteredOn(alert -> "FA-001".equals(alert.code()))
                .singleElement()
                .extracting(FinancialAlert::severity)
                .isEqualTo("CRITICAL");
        assertThat(alerts).filteredOn(alert -> !"FA-001".equals(alert.code()))
                .extracting(FinancialAlert::severity)
                .containsOnly("WARNING");

        verify(summaryService).getSummaryMonthlyByUserIdAndDate(userId, month);
        verify(summaryService).getSummaryMonthlyByUserIdAndDate(userId, month.minusMonths(1));
        verify(transactionService).getByUserAndDate(userId, month.atDay(1), month.atEndOfMonth());
    }
}
