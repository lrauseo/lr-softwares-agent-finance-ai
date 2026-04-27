package com.lrsoftwares.finance_ai_agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.entity.ChatMessage;
import com.lrsoftwares.finance_ai_agent.entity.ChatRole;
import com.lrsoftwares.finance_ai_agent.entity.ChatSession;
import com.lrsoftwares.finance_ai_agent.repository.ChatMessageRepository;
import com.lrsoftwares.finance_ai_agent.repository.ChatSessionRepository;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    private ChatService chatService;

    @Test
    void createSessionShouldPersistSessionWithDefaultTitle() {
        UUID userId = UUID.randomUUID();
        ChatSession persisted = new ChatSession();
        persisted.setId(UUID.randomUUID());
        persisted.setUserId(userId);
        persisted.setTitle("Nova conversa");

        when(authenticatedUserProvider.getUserId()).thenReturn(userId);
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(persisted);

        ChatSession result = chatService.createSession();

        assertThat(result.getId()).isEqualTo(persisted.getId());
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTitle()).isEqualTo("Nova conversa");

        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getTitle()).isEqualTo("Nova conversa");
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    void listSessionsShouldReturnRepositoryResult() {
        UUID userId = UUID.randomUUID();

        ChatSession first = new ChatSession();
        first.setId(UUID.randomUUID());
        first.setUserId(userId);
        first.setTitle("Conversa A");

        ChatSession second = new ChatSession();
        second.setId(UUID.randomUUID());
        second.setUserId(userId);
        second.setTitle("Conversa B");

        when(authenticatedUserProvider.getUserId()).thenReturn(userId);
        when(sessionRepository.findByUserId(userId)).thenReturn(List.of(first, second));

        List<ChatSession> sessions = chatService.listSessions();

        assertThat(sessions).hasSize(2);
        assertThat(sessions).extracting(ChatSession::getTitle).containsExactly("Conversa A", "Conversa B");
        verify(sessionRepository).findByUserId(userId);
    }

    @Test
    void getMessagesShouldReturnMessagesOrderedFromRepository() {
        UUID sessionId = UUID.randomUUID();

        ChatMessage first = new ChatMessage();
        first.setId(UUID.randomUUID());
        first.setSessionId(sessionId);
        first.setRole(ChatRole.USER);
        first.setContent("Pergunta inicial");

        ChatMessage second = new ChatMessage();
        second.setId(UUID.randomUUID());
        second.setSessionId(sessionId);
        second.setRole(ChatRole.ASSISTANT);
        second.setContent("Resposta inicial");

        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)).thenReturn(List.of(first, second));

        List<ChatMessage> messages = chatService.getMessages(sessionId);

        assertThat(messages).hasSize(2);
        assertThat(messages).extracting(ChatMessage::getRole).containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
        verify(messageRepository).findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Test
    void saveMessageShouldPersistMessageWithSessionRoleAndContent() {
        UUID sessionId = UUID.randomUUID();
        String content = "Preciso reduzir gastos com lazer.";

        chatService.saveMessage(sessionId, ChatRole.USER, content);

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepository).save(captor.capture());

        ChatMessage saved = captor.getValue();
        assertThat(saved.getSessionId()).isEqualTo(sessionId);
        assertThat(saved.getRole()).isEqualTo(ChatRole.USER);
        assertThat(saved.getContent()).isEqualTo(content);
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}
