package com.lrsoftwares.finance_ai_agent.repository.rag;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lrsoftwares.finance_ai_agent.entity.rag.KnowledgeChunk;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

	boolean existsBySource(String source);

	void deleteBySource(String source);

	List<KnowledgeChunk> findTop5ByLanguageAndContentContainingIgnoreCase(
			String language,
			String query);

	List<KnowledgeChunk> findTop10ByLanguageAndContentContainingIgnoreCase(
			String language,
			String query);

	List<KnowledgeChunk> findTop10ByLanguageAndSourceInAndContentContainingIgnoreCase(
			String language,
			List<String> sources,
			String query);
}