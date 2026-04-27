package com.lrsoftwares.finance_ai_agent.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.entity.ChatMessage;
import com.lrsoftwares.finance_ai_agent.entity.ChatRole;
import com.lrsoftwares.finance_ai_agent.entity.ChatSession;
import com.lrsoftwares.finance_ai_agent.repository.ChatMessageRepository;
import com.lrsoftwares.finance_ai_agent.repository.ChatSessionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ChatSession createSession() {
        UUID userId = authenticatedUserProvider.getUserId();
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle("Nova conversa");
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        return sessionRepository.save(session);
    }

    public List<ChatSession> listSessions() {
        UUID userId = authenticatedUserProvider.getUserId();
        return sessionRepository.findByUserId(userId);
    }

    public List<ChatMessage> getMessages(UUID sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    public void saveMessage(UUID sessionId, ChatRole role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());

        messageRepository.save(msg);
    }
}