package com.lrsoftwares.finance_ai_agent.entity.rag;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "knowledge_chunks")
@Data
public class KnowledgeChunk {

	@Id
	@GeneratedValue
	private UUID id;

	private String source;
	private String theme;
	private String audience;
	private String language;

	@Column(columnDefinition = "TEXT")
	private String content;
	@CreationTimestamp
	private LocalDateTime createdAt;
}
