package com.lrsoftwares.finance_ai_agent.service.rag;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.dto.rag.KnowledgeSearchResponse;
import com.lrsoftwares.finance_ai_agent.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class KnowledgeRetrievalService {

	private final VectorStore vectorStore;

	public List<Document> retrieve(@NonNull String question) {
		FilterExpressionBuilder filter = new FilterExpressionBuilder();
		var expression = filter.and(filter.eq("language", "pt-BR"),
				filter.or(filter.eq("theme", "educacao_financeira"),
						filter.in("tags", "educacao_financeira")))
				.build();

		try {
			List<Document> primaryResults = vectorStore.similaritySearch(
					SearchRequest.builder()
							.query(question)
							.topK(5)
							.similarityThreshold(0.70)
							.filterExpression(expression)
							.build());

			if (primaryResults != null && !primaryResults.isEmpty()) {
				return primaryResults;
			}

			List<Document> relaxedResults = vectorStore.similaritySearch(
					SearchRequest.builder()
							.query(question)
							.topK(8)
							.similarityThreshold(0.45)
							.filterExpression(expression)
							.build());

			if (relaxedResults != null && !relaxedResults.isEmpty()) {
				log.info("RAG retrieve sem resultado em threshold 0.70; retornando fallback com threshold 0.45.");
				return relaxedResults;
			}

			log.info("RAG retrieve sem resultados com filtro de tema/tags; executando fallback sem filtro.");
			return vectorStore.similaritySearch(
					SearchRequest.builder()
							.query(question)
							.topK(8)
							.similarityThreshold(0.35)
							.build());
		} catch (DataIntegrityViolationException ex) {
			if (isVectorDimensionMismatch(ex)) {
				log.error("Falha no RAG por incompatibilidade de dimensao do pgvector (embedding x coluna). "
						+ "Alinhe modelo de embedding e dimensao da tabela vector_store antes de consultar.", ex);
				throw new BusinessException(
						"Incompatibilidade de dimensao no pgvector. Verifique OPENAI_EMBEDDING_MODEL/OPENAI_EMBEDDING_DIMENSIONS e reingestao.");
			}
			throw ex;
		}
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
		
		try {
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
		} catch (DataIntegrityViolationException ex) {
			if (isVectorDimensionMismatch(ex)) {
				log.error("Falha no endpoint de busca semantica por incompatibilidade de dimensao do pgvector.", ex);
				throw new BusinessException(
						"Incompatibilidade de dimensao no pgvector. Verifique OPENAI_EMBEDDING_MODEL/OPENAI_EMBEDDING_DIMENSIONS e reingestao.");
			}
			throw ex;
		}

	}

	private boolean isVectorDimensionMismatch(DataIntegrityViolationException ex) {
		String message = ex.getMessage();
		return message != null && message.toLowerCase().contains("different vector dimensions");
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