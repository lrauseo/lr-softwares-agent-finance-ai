package com.lrsoftwares.finance_ai_agent.service.rag;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.entity.rag.KnowledgeChunk;
import com.lrsoftwares.finance_ai_agent.repository.rag.KnowledgeChunkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

	private final KnowledgeChunkRepository repository;

	public List<KnowledgeChunk> retrieve(String question) {
		return repository.findTop5ByLanguageAndContentContainingIgnoreCase(
				"pt-BR",
				question);
	}

	public List<KnowledgeChunk> search(String query, String language, List<String> sources) {
		String normalizedLanguage = Objects.requireNonNullElse(language, "pt-BR");
		if (sources == null || sources.isEmpty()) {
			return repository.findTop10ByLanguageAndContentContainingIgnoreCase(normalizedLanguage, query);
		}

		return repository.findTop10ByLanguageAndSourceInAndContentContainingIgnoreCase(
				normalizedLanguage,
				sources,
				query);
	}
}