package com.lrsoftwares.finance_ai_agent.dto;

public record FinancialAlert(
    String code,
    String message,
    String severity
) {}