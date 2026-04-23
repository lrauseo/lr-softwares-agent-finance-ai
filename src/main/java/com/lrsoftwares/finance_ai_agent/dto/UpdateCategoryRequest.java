package com.lrsoftwares.finance_ai_agent.dto;

import com.lrsoftwares.finance_ai_agent.entity.TransactionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateCategoryRequest(
        @NotBlank String name,
        @NotNull TransactionType type
) {}
