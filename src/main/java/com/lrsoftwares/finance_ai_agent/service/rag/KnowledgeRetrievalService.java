package com.lrsoftwares.finance_ai_agent.service.rag;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.dto.rag.KnowledgeSearchResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

	private final VectorStore vectorStore;

	public List<Document> retrieve(@NonNull String question) {
		return vectorStore.similaritySearch(
				SearchRequest.builder()
						.query(question)
						.topK(5)
						.similarityThreshold(0.70)
						.filterExpression("language == 'pt-BR'").build());
	}

	public List<KnowledgeSearchResponse> search(String query, String language, List<String> sources) {
		String normalizedLanguage = Objects.requireNonNullElse(language, "pt-BR");
		String filter = "language == '" + normalizedLanguage + "'";
		if (sources != null && !sources.isEmpty()) {
			String sourceFilter = sources.stream()
					.map(source -> "source == '" + source + "'")
					.reduce((a, b) -> a + " || " + b)
					.orElse("");
			filter += " && (" + sourceFilter + ")";
		}
		return vectorStore.similaritySearch(
				SearchRequest.builder()
						.query(Objects.requireNonNull(query))
						.topK(10)
						.similarityThreshold(0.65)
						.filterExpression(filter)
						.build())
				.stream()
				.map(this::toKnowledgeSearchResponse)
				.toList();

	}

	private KnowledgeSearchResponse toKnowledgeSearchResponse(Document document) {
		Map<String, Object> metadata = document.getMetadata();

		return new KnowledgeSearchResponse(
				document.getText(),
				getMetadataValue(metadata, "source"),
				getMetadataValue(metadata, "theme"),
				getMetadataValue(metadata, "audience"),
				getMetadataValue(metadata, "language"),
				metadata);
	}

	private String getMetadataValue(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		return value != null ? value.toString() : null;
	}
}