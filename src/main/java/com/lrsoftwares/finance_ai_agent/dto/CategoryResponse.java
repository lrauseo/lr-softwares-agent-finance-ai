package com.lrsoftwares.finance_ai_agent.dto;

public record CategoryResponse(
		String id,
		String name,
		String type,
		String userId,
		Boolean systemDefault) {
}