package com.lrsoftwares.finance_ai_agent.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;

public record TransactionResponse(
		UUID id,
		UUID userId,
		Category category,
		LocalDate date,
		BigDecimal amount,
		TransactionType type,
		String description,
		Boolean recurring) {}
