package com.lrsoftwares.finance_ai_agent.dto.rag;

import jakarta.validation.constraints.NotBlank;

public record CreateKnowledgeChunkRequest(
        @NotBlank String source,
        @NotBlank String theme,
        @NotBlank String audience,
        @NotBlank String language,
        @NotBlank String content) {
}
