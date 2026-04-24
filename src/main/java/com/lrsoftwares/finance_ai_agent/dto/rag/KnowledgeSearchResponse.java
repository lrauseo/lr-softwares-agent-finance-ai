package com.lrsoftwares.finance_ai_agent.dto.rag;

import java.util.Map;

public record KnowledgeSearchResponse(
        String content,
        String source,
        String theme,
        String audience,
        String language,
        Map<String, Object> metadata
) {}
