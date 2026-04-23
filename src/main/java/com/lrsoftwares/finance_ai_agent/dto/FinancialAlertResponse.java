package com.lrsoftwares.finance_ai_agent.dto;

import java.math.BigDecimal;

import com.lrsoftwares.finance_ai_agent.entity.AlertSeverity;

public record FinancialAlertResponse(
        AlertSeverity severity,
        String code,
        String title,
        String message,
        BigDecimal value
) {}
