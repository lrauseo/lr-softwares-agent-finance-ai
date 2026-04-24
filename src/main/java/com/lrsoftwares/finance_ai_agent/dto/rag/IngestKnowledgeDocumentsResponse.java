package com.lrsoftwares.finance_ai_agent.dto.rag;

import java.util.List;

public record IngestKnowledgeDocumentsResponse(
        int ingestedCount,
        List<KnowledgeChunkResponse> chunks) {
}
