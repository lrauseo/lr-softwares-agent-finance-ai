package com.lrsoftwares.finance_ai_agent.dto;

import java.math.BigDecimal;

public record CategoryTotalResponse(
    String category,
    BigDecimal total
) {}