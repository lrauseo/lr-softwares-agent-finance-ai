package com.lrsoftwares.finance_ai_agent.dto.sprint8;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateCategoryBudgetRequest(
        @NotNull UUID categoryId,
        @NotNull @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
        @NotNull @DecimalMin(value = "0.01") BigDecimal plannedAmount,
        @DecimalMin(value = "0.50") BigDecimal alertThresholdRate
) {
}
