package com.lrsoftwares.finance_ai_agent.dto.sprint8;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFinancialGoalRequest(
        @NotBlank String name,
        @NotNull @DecimalMin(value = "0.01") BigDecimal targetAmount,
        @DecimalMin(value = "0.00") BigDecimal currentAmount,
        @NotNull LocalDate targetDate
) {
}
