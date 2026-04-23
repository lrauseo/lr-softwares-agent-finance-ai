package com.lrsoftwares.finance_ai_agent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.lrsoftwares.finance_ai_agent.entity.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateTransactionRequest(
        @NotNull UUID categoryId,
        @NotNull LocalDate date,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull TransactionType type,
        String description,
        boolean recurring
) {}
