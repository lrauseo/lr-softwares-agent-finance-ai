package com.lrsoftwares.finance_ai_agent.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.lrsoftwares.finance_ai_agent.entity.ChatMessage;
import com.lrsoftwares.finance_ai_agent.entity.ChatRole;
import com.lrsoftwares.finance_ai_agent.entity.ChatSession;
import com.lrsoftwares.finance_ai_agent.service.ChatService;
import com.lrsoftwares.finance_ai_agent.service.ai.FinancialAdvisorChatService;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @MockBean
    private FinancialAdvisorChatService advisorService;

    @Test
    void shouldCreateSession() throws Exception {
        UUID sessionId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        ChatSession session = new ChatSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setTitle("Nova conversa");
        session.setCreatedAt(LocalDateTime.of(2026, 4, 24, 10, 0));
        session.setUpdatedAt(LocalDateTime.of(2026, 4, 24, 10, 0));

        when(chatService.createSession(userId)).thenReturn(session);

        mockMvc.perform(post("/api/chat/sessions")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.title").value("Nova conversa"));

        verify(chatService).createSession(userId);
    }

    @Test
    void shouldListSessionsByUser() throws Exception {
        UUID userId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

        ChatSession first = new ChatSession();
        first.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        first.setUserId(userId);
        first.setTitle("Conversa 1");
        first.setCreatedAt(LocalDateTime.of(2026, 4, 20, 9, 0));
        first.setUpdatedAt(LocalDateTime.of(2026, 4, 20, 9, 0));

        ChatSession second = new ChatSession();
        second.setId(UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc"));
        second.setUserId(userId);
        second.setTitle("Conversa 2");
        second.setCreatedAt(LocalDateTime.of(2026, 4, 21, 9, 0));
        second.setUpdatedAt(LocalDateTime.of(2026, 4, 21, 9, 0));

        when(chatService.listSessions(userId)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/chat/sessions")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Conversa 1"))
                .andExpect(jsonPath("$[1].title").value("Conversa 2"));

        verify(chatService).listSessions(userId);
    }

    @Test
    void shouldGetMessagesFromSession() throws Exception {
        UUID sessionId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        ChatMessage first = new ChatMessage();
        first.setId(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd"));
        first.setSessionId(sessionId);
        first.setRole(ChatRole.USER);
        first.setContent("Como economizar este mes?");
        first.setCreatedAt(LocalDateTime.of(2026, 4, 24, 10, 0));

        ChatMessage second = new ChatMessage();
        second.setId(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"));
        second.setSessionId(sessionId);
        second.setRole(ChatRole.ASSISTANT);
        second.setContent("Vamos comecar por um teto de gastos semanal.");
        second.setCreatedAt(LocalDateTime.of(2026, 4, 24, 10, 1));

        when(chatService.getMessages(sessionId)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/chat/sessions/{id}/messages", sessionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[1].role").value("ASSISTANT"));

        verify(chatService).getMessages(eq(sessionId));
    }
}
