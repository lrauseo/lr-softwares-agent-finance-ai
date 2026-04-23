package com.lrsoftwares.finance_ai_agent.dto;

import java.util.UUID;

import com.lrsoftwares.finance_ai_agent.entity.TransactionType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCategoryRequest(
    @NotNull UUID userId,
    @NotBlank String name,
    @NotNull TransactionType type
) {}
