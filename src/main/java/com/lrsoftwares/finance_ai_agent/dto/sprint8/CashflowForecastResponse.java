package com.lrsoftwares.finance_ai_agent.dto.sprint8;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record CashflowForecastResponse(
        YearMonth baseMonth,
        List<ProjectionItem> projections,
        String method
) {
    public record ProjectionItem(
            YearMonth month,
            BigDecimal expectedIncome,
            BigDecimal expectedExpense,
            BigDecimal expectedBalance
    ) {
    }
}
