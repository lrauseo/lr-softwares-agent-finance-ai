package com.lrsoftwares.finance_ai_agent.dto;

import java.util.UUID;

import com.lrsoftwares.finance_ai_agent.entity.TransactionType;

public record CategoryResponse(
        UUID id,
        UUID userId,
        String name,
        TransactionType type,
        Boolean systemDefault
) {}
