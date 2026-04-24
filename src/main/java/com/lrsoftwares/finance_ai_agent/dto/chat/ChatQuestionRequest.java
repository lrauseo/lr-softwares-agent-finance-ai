package com.lrsoftwares.finance_ai_agent.dto.chat;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatQuestionRequest(@NotNull UUID userId, @NotBlank String question) {
}