package com.lrsoftwares.finance_ai_agent.service.analysis;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lrsoftwares.finance_ai_agent.dto.CategoryTotalResponse;
import com.lrsoftwares.finance_ai_agent.dto.FinancialDiagnosisResponse;
import com.lrsoftwares.finance_ai_agent.dto.MonthlySummaryResponse;
import com.lrsoftwares.finance_ai_agent.entity.AlertSeverity;
import com.lrsoftwares.finance_ai_agent.service.SummaryService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialAnalysisServiceImplTest {

    @Mock
    private SummaryService summaryService;

    private FinancialAnalysisServiceImpl analysisService;

    @BeforeEach
    void setUp() {
        analysisService = new FinancialAnalysisServiceImpl(summaryService);
    }

    @Test
    void testAnalyzeMonthly_PositiveBalance() {
        UUID userId = UUID.randomUUID();
        YearMonth month = YearMonth.now();

        CategoryTotalResponse category = new CategoryTotalResponse("Alimentação", BigDecimal.valueOf(2200.00));
        MonthlySummaryResponse summary = new MonthlySummaryResponse(
                BigDecimal.valueOf(5000.00),
                BigDecimal.valueOf(4800.00),
                BigDecimal.valueOf(200.00),
                Arrays.asList(category)
        );

        when(summaryService.getSummaryMonthlyByUserIdAndDate(userId, month)).thenReturn(summary);

        FinancialDiagnosisResponse diagnosis = analysisService.analyzeMonthly(userId, month);

        assertThat(diagnosis).isNotNull();
        assertThat(diagnosis.userId()).isEqualTo(userId);
        assertThat(diagnosis.month()).isEqualTo(month);
        assertThat(diagnosis.summary().totalIncome()).isEqualByComparingTo(BigDecimal.valueOf(5000.00));
        assertThat(diagnosis.summary().totalExpense()).isEqualByComparingTo(BigDecimal.valueOf(4800.00));
        assertThat(diagnosis.summary().balance()).isEqualByComparingTo(BigDecimal.valueOf(200.00));
        assertThat(diagnosis.alerts()).isNotNull();
        assertThat(diagnosis.highlights()).isNotNull();
        assertThat(diagnosis.recommendations()).isNotNull();
    }

    @Test
    void testAnalyzeMonthly_ExpensesGreaterThanIncome() {
        UUID userId = UUID.randomUUID();
        YearMonth month = YearMonth.now();

        CategoryTotalResponse category = new CategoryTotalResponse("Alimentação", BigDecimal.valueOf(6000.00));
        MonthlySummaryResponse summary = new MonthlySummaryResponse(
                BigDecimal.valueOf(5000.00),
                BigDecimal.valueOf(6000.00),
                BigDecimal.valueOf(-1000.00),
                Arrays.asList(category)
        );

        when(summaryService.getSummaryMonthlyByUserIdAndDate(userId, month)).thenReturn(summary);

        FinancialDiagnosisResponse diagnosis = analysisService.analyzeMonthly(userId, month);

        assertThat(diagnosis.alerts()).isNotEmpty();
        assertThat(diagnosis.alerts().get(0).code()).isEqualTo("EXPENSES_GREATER_THAN_INCOME");
        assertThat(diagnosis.alerts().get(0).severity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void testAnalyzeMonthly_LowBalanceRate() {
        UUID userId = UUID.randomUUID();
        YearMonth month = YearMonth.now();

        CategoryTotalResponse category = new CategoryTotalResponse("Alimentação", BigDecimal.valueOf(4500.00));
        MonthlySummaryResponse summary = new MonthlySummaryResponse(
                BigDecimal.valueOf(5000.00),
                BigDecimal.valueOf(4800.00),
                BigDecimal.valueOf(200.00),
                Arrays.asList(category)
        );

        when(summaryService.getSummaryMonthlyByUserIdAndDate(userId, month)).thenReturn(summary);

        FinancialDiagnosisResponse diagnosis = analysisService.analyzeMonthly(userId, month);

        assertThat(diagnosis.alerts()).isNotEmpty();
        assertThat(diagnosis.alerts().stream()
                .anyMatch(alert -> alert.code().equals("LOW_BALANCE_RATE")))
                .isTrue();
    }

    @Test
    void testAnalyzeMonthly_CategoryOver50Percent() {
        UUID userId = UUID.randomUUID();
        YearMonth month = YearMonth.now();

        CategoryTotalResponse category = new CategoryTotalResponse("Alimentação", BigDecimal.valueOf(2600.00));
        MonthlySummaryResponse summary = new MonthlySummaryResponse(
                BigDecimal.valueOf(5000.00),
                BigDecimal.valueOf(2600.00),
                BigDecimal.valueOf(2400.00),
                Arrays.asList(category)
        );

        when(summaryService.getSummaryMonthlyByUserIdAndDate(userId, month)).thenReturn(summary);

        FinancialDiagnosisResponse diagnosis = analysisService.analyzeMonthly(userId, month);

        assertThat(diagnosis.alerts()).isNotEmpty();
        assertThat(diagnosis.alerts().stream()
                .anyMatch(alert -> alert.code().equals("CATEGORY_OVER_50_PERCENT_INCOME")))
                .isTrue();
    }

    @Test
    void testAnalyzeMonthly_AlertsSortedBySeverity() {
        UUID userId = UUID.randomUUID();
        YearMonth month = YearMonth.now();

        CategoryTotalResponse category = new CategoryTotalResponse("Alimentação", BigDecimal.valueOf(2600.00));
        MonthlySummaryResponse summary = new MonthlySummaryResponse(
                BigDecimal.valueOf(5000.00),
                BigDecimal.valueOf(2600.00),
                BigDecimal.valueOf(2400.00),
                Arrays.asList(category)
        );

        when(summaryService.getSummaryMonthlyByUserIdAndDate(userId, month)).thenReturn(summary);

        FinancialDiagnosisResponse diagnosis = analysisService.analyzeMonthly(userId, month);

        for (int i = 1; i < diagnosis.alerts().size(); i++) {
            assertThat(diagnosis.alerts().get(i - 1).severity().ordinal())
                    .isGreaterThanOrEqualTo(diagnosis.alerts().get(i).severity().ordinal());
        }
    }
}
