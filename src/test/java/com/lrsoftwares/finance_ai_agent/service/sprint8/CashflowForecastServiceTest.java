package com.lrsoftwares.finance_ai_agent.service.sprint8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lrsoftwares.finance_ai_agent.dto.MonthlySummaryResponse;
import com.lrsoftwares.finance_ai_agent.service.SummaryService;

@ExtendWith(MockitoExtension.class)
class CashflowForecastServiceTest {

    @Mock
    private SummaryService summaryService;

    @InjectMocks
    private CashflowForecastService service;

    @Test
    void shouldGenerateProjectionForRequestedMonths() {
        YearMonth baseMonth = YearMonth.of(2026, 4);

        when(summaryService.getSummaryMonthlyByUserIdAndDate(baseMonth.minusMonths(2)))
                .thenReturn(new MonthlySummaryResponse(new BigDecimal("5000"), new BigDecimal("3500"), new BigDecimal("1500"), List.of()));
        when(summaryService.getSummaryMonthlyByUserIdAndDate(baseMonth.minusMonths(1)))
                .thenReturn(new MonthlySummaryResponse(new BigDecimal("5100"), new BigDecimal("3600"), new BigDecimal("1500"), List.of()));
        when(summaryService.getSummaryMonthlyByUserIdAndDate(baseMonth))
                .thenReturn(new MonthlySummaryResponse(new BigDecimal("5300"), new BigDecimal("3700"), new BigDecimal("1600"), List.of()));

        var response = service.forecast(baseMonth, 3);

        assertThat(response.projections()).hasSize(3);
        assertThat(response.projections().get(0).month()).isEqualTo(baseMonth.plusMonths(1));
        assertThat(response.projections().get(2).expectedIncome()).isPositive();
    }
}
