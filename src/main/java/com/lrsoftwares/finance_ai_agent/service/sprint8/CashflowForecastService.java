package com.lrsoftwares.finance_ai_agent.service.sprint8;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.dto.MonthlySummaryResponse;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.CashflowForecastResponse;
import com.lrsoftwares.finance_ai_agent.service.SummaryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CashflowForecastService {

    private static final int DEFAULT_HISTORY_MONTHS = 3;
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final SummaryService summaryService;

    public CashflowForecastResponse forecast(YearMonth baseMonth, int monthsAhead) {
        int horizon = Math.max(1, monthsAhead);
        List<MonthlySummaryResponse> history = new ArrayList<>();

        for (int i = DEFAULT_HISTORY_MONTHS - 1; i >= 0; i--) {
            history.add(summaryService.getSummaryMonthlyByUserIdAndDate(baseMonth.minusMonths(i)));
        }

        BigDecimal avgIncome = history.stream()
                .map(MonthlySummaryResponse::totalIncome)
                .reduce(ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);

        BigDecimal avgExpense = history.stream()
                .map(MonthlySummaryResponse::totalExpense)
                .reduce(ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(history.size()), 2, RoundingMode.HALF_UP);

        BigDecimal incomeTrend = history.getLast().totalIncome().subtract(history.getFirst().totalIncome())
                .divide(BigDecimal.valueOf(history.size() - 1L), 2, RoundingMode.HALF_UP);
        BigDecimal expenseTrend = history.getLast().totalExpense().subtract(history.getFirst().totalExpense())
                .divide(BigDecimal.valueOf(history.size() - 1L), 2, RoundingMode.HALF_UP);

        List<CashflowForecastResponse.ProjectionItem> projections = new ArrayList<>();

        for (int i = 1; i <= horizon; i++) {
            BigDecimal expectedIncome = maxZero(avgIncome.add(incomeTrend.multiply(BigDecimal.valueOf(i))));
            BigDecimal expectedExpense = maxZero(avgExpense.add(expenseTrend.multiply(BigDecimal.valueOf(i))));
            BigDecimal expectedBalance = expectedIncome.subtract(expectedExpense);

            projections.add(new CashflowForecastResponse.ProjectionItem(
                    baseMonth.plusMonths(i),
                    expectedIncome,
                    expectedExpense,
                    expectedBalance));
        }

        return new CashflowForecastResponse(baseMonth, projections, "moving-average-with-linear-trend");
    }

    private BigDecimal maxZero(BigDecimal value) {
        return value.compareTo(ZERO) < 0 ? ZERO : value;
    }
}
