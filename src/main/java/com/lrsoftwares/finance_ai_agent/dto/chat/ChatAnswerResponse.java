package com.lrsoftwares.finance_ai_agent.dto.chat;

import java.time.LocalDateTime;
import java.time.YearMonth;

import org.springframework.lang.NonNull;

public record ChatAnswerResponse(@NonNull String answer, @NonNull YearMonth monthReference,
		@NonNull LocalDateTime generatedAt) {
}