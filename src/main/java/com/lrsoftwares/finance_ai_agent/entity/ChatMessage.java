package com.lrsoftwares.finance_ai_agent.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessage {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    private ChatRole role; // USER | ASSISTANT

    @Column(columnDefinition = "TEXT")
    private String content;
	@CreationTimestamp
    private LocalDateTime createdAt;
}