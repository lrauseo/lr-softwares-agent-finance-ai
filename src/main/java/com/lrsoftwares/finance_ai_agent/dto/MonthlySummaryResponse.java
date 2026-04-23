package com.lrsoftwares.finance_ai_agent.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlySummaryResponse(
    BigDecimal totalIncome,
    BigDecimal totalExpense,
    BigDecimal balance,
    List<CategoryTotalResponse> categories
) {}