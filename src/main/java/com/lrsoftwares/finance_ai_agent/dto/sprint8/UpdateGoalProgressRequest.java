package com.lrsoftwares.finance_ai_agent.dto.sprint8;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateGoalProgressRequest(
        @NotNull @DecimalMin(value = "0.00") BigDecimal currentAmount
) {
}
