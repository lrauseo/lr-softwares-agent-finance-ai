package com.lrsoftwares.finance_ai_agent.dto.sprint8;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.lrsoftwares.finance_ai_agent.entity.GoalStatus;

public record FinancialGoalResponse(
        UUID id,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        BigDecimal progressRate,
        LocalDate targetDate,
        GoalStatus status
) {
}
