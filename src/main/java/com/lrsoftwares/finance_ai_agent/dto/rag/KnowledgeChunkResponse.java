package com.lrsoftwares.finance_ai_agent.dto.rag;

import java.time.LocalDateTime;
import java.util.UUID;

public record KnowledgeChunkResponse(
        UUID id,
        String source,
        String theme,
        String audience,
        String language,
        String content,
        LocalDateTime createdAt) {
}
