package com.lrsoftwares.finance_ai_agent.dto.sprint8;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record CategoryBudgetResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        YearMonth month,
        BigDecimal plannedAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        BigDecimal consumedRate,
        BigDecimal alertThresholdRate,
        boolean exceeded
) {
}
