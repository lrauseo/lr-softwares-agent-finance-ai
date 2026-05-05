package com.lrsoftwares.finance_ai_agent.service.rag;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.dto.rag.KnowledgeSearchResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

	private final VectorStore vectorStore;

	public List<Document> retrieve(@NonNull String question) {
		FilterExpressionBuilder filter = new FilterExpressionBuilder();
		var expression = filter.and(filter.eq("language", "pt-BR"),
				filter.or(filter.eq("theme", "educacao_financeira"),
						filter.in("tags", "educacao_financeira")))
				.build();

		return vectorStore.similaritySearch(
				SearchRequest.builder()
						.query(question)
						.topK(5)
						.similarityThreshold(0.70)
						.filterExpression(expression)
						.build());
	}

	public List<KnowledgeSearchResponse> search(String query, String language, List<String> sources) {
		String normalizedLanguage = Objects.requireNonNullElse(language, "pt-BR");
		
		FilterExpressionBuilder filterBuilder = new FilterExpressionBuilder();
		
		var baseExpression = filterBuilder.and(
				filterBuilder.eq("language", normalizedLanguage),
				filterBuilder.or(filterBuilder.eq("theme", "educacao_financeira"), 
						filterBuilder.in("tags", "educacao_financeira")));
		
		var expression = (sources != null && !sources.isEmpty())
				? filterBuilder.and(baseExpression, filterBuilder.in("source", sources)).build()
				: baseExpression.build();
		
		return vectorStore.similaritySearch(
				SearchRequest.builder()
						.query(Objects.requireNonNull(query))
						.topK(10)
						.similarityThreshold(0.65)
						.filterExpression(expression)
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