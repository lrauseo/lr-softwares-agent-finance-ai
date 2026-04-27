package com.lrsoftwares.finance_ai_agent.service.ai.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lrsoftwares.finance_ai_agent.dto.CategoryTotalResponse;
import com.lrsoftwares.finance_ai_agent.dto.FinancialAlertResponse;
import com.lrsoftwares.finance_ai_agent.dto.FinancialDiagnosisResponse;
import com.lrsoftwares.finance_ai_agent.dto.MonthlySummaryResponse;
import com.lrsoftwares.finance_ai_agent.dto.chat.ChatAnswerResponse;
import com.lrsoftwares.finance_ai_agent.dto.chat.ChatQuestionRequest;
import com.lrsoftwares.finance_ai_agent.entity.AlertSeverity;
import com.lrsoftwares.finance_ai_agent.entity.ChatMessage;
import com.lrsoftwares.finance_ai_agent.entity.ChatRole;
import com.lrsoftwares.finance_ai_agent.service.ChatService;
import com.lrsoftwares.finance_ai_agent.service.ai.LLMClient;
import com.lrsoftwares.finance_ai_agent.service.analysis.FinancialAnalysisService;
import com.lrsoftwares.finance_ai_agent.service.rag.KnowledgeRetrievalService;

@ExtendWith(MockitoExtension.class)
class FinancialAdvisorChatServiceImplTest {

    @Mock
    private FinancialAnalysisService analysisService;

    @Mock
    private LLMClient llmClient;

    @Mock
    private ChatService chatService;

    @Mock
    private KnowledgeRetrievalService knowledgeRetrievalService;

    private FinancialAdvisorChatServiceImpl advisorService;

    @BeforeEach
    void setUp() {
        advisorService = new FinancialAdvisorChatServiceImpl(
                analysisService,
                llmClient,
                chatService,
                knowledgeRetrievalService);
    }

    @Test
    void answerShouldUseHistoryAsContextAndPersistMessages() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ChatQuestionRequest request = new ChatQuestionRequest(userId, "Como melhorar meu saldo?");

        ChatMessage previousUser = new ChatMessage();
        previousUser.setSessionId(sessionId);
        previousUser.setRole(ChatRole.USER);
        previousUser.setContent("No mes passado gastei muito em delivery.");

        ChatMessage previousAssistant = new ChatMessage();
        previousAssistant.setSessionId(sessionId);
        previousAssistant.setRole(ChatRole.ASSISTANT);
        previousAssistant.setContent("Vamos definir um limite semanal para alimentacao.");

        ChatMessage currentUser = new ChatMessage();
        currentUser.setSessionId(sessionId);
        currentUser.setRole(ChatRole.USER);
        currentUser.setContent("Como melhorar meu saldo?");

        when(chatService.getMessages(sessionId)).thenReturn(List.of(previousUser, previousAssistant, currentUser));
        when(analysisService.analyzeMonthly(eq(userId), any(YearMonth.class))).thenReturn(buildDiagnosis(userId));
        when(knowledgeRetrievalService.retrieve("Como melhorar meu saldo?")).thenReturn(List.of(buildChunk()));
        when(llmClient.generate(any(String.class), any(String.class))).thenReturn("Comece cortando 15% dos gastos variaveis.");

        ChatAnswerResponse response = advisorService.answer(sessionId, request);

        assertThat(response.answer()).isEqualTo("Comece cortando 15% dos gastos variaveis.");
        assertThat(response.monthReference()).isEqualTo(YearMonth.now());
        assertThat(response.generatedAt()).isNotNull();

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).generate(systemPromptCaptor.capture(), userPromptCaptor.capture());

        String systemPrompt = systemPromptCaptor.getValue();
        String userPrompt = userPromptCaptor.getValue();

        assertThat(systemPrompt).contains("Histórico da conversa:");
        assertThat(systemPrompt).contains("USER: No mes passado gastei muito em delivery.");
        assertThat(systemPrompt).contains("ASSISTANT: Vamos definir um limite semanal para alimentacao.");
        assertThat(systemPrompt).contains("Receita total:");
        assertThat(systemPrompt).contains("Conhecimento interno recuperado:");
        assertThat(userPrompt).contains("Como melhorar meu saldo?");

        verify(chatService).saveMessage(sessionId, ChatRole.USER, "Como melhorar meu saldo?");
        verify(chatService).saveMessage(sessionId, ChatRole.ASSISTANT, "Comece cortando 15% dos gastos variaveis.");
    }

    @Test
    void answerShouldKeepContextBetweenConsecutiveQuestions() {
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ChatQuestionRequest firstQuestion = new ChatQuestionRequest(userId, "Primeira pergunta");
        ChatQuestionRequest secondQuestion = new ChatQuestionRequest(userId, "Segunda pergunta");

        when(analysisService.analyzeMonthly(eq(userId), any(YearMonth.class))).thenReturn(buildDiagnosis(userId));
        when(knowledgeRetrievalService.retrieve(any(String.class))).thenReturn(List.of(buildChunk()));

        when(chatService.getMessages(sessionId))
                .thenReturn(List.of(message(ChatRole.USER, "Primeira pergunta")))
                .thenReturn(List.of(
                        message(ChatRole.USER, "Primeira pergunta"),
                        message(ChatRole.ASSISTANT, "Resposta da primeira"),
                        message(ChatRole.USER, "Segunda pergunta")));

        when(llmClient.generate(any(String.class), any(String.class)))
                .thenReturn("Resposta da primeira")
                .thenReturn("Resposta da segunda");

        advisorService.answer(sessionId, firstQuestion);
        advisorService.answer(sessionId, secondQuestion);

        ArgumentCaptor<String> systemPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient, times(2)).generate(systemPromptCaptor.capture(), any(String.class));

        List<String> allSystemPrompts = systemPromptCaptor.getAllValues();
        String secondPrompt = allSystemPrompts.get(1);

        assertThat(secondPrompt).contains("USER: Primeira pergunta");
        assertThat(secondPrompt).contains("ASSISTANT: Resposta da primeira");
        assertThat(secondPrompt).contains("USER: Segunda pergunta");

        verify(chatService).saveMessage(sessionId, ChatRole.USER, "Primeira pergunta");
        verify(chatService).saveMessage(sessionId, ChatRole.ASSISTANT, "Resposta da primeira");
        verify(chatService).saveMessage(sessionId, ChatRole.USER, "Segunda pergunta");
        verify(chatService).saveMessage(sessionId, ChatRole.ASSISTANT, "Resposta da segunda");
    }

    private FinancialDiagnosisResponse buildDiagnosis(UUID userId) {
        MonthlySummaryResponse summary = new MonthlySummaryResponse(
                new BigDecimal("5000.00"),
                new BigDecimal("4200.00"),
                new BigDecimal("800.00"),
                List.of(new CategoryTotalResponse("Moradia", new BigDecimal("1500.00"))));

        FinancialAlertResponse alert = new FinancialAlertResponse(
                AlertSeverity.WARNING,
                "LOW_BALANCE_RATE",
                "Saldo apertado",
                "Seu saldo esta abaixo de 20% da receita.",
                new BigDecimal("0.16"));

        return new FinancialDiagnosisResponse(
                userId,
                YearMonth.now(),
                summary,
                List.of(alert),
                List.of("Maior gasto na categoria Moradia."),
                List.of("Defina um limite de despesas variaveis."));
    }

    private ChatMessage message(ChatRole role, String content) {
        ChatMessage message = new ChatMessage();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    private Document buildChunk() {
        return new Document(
                "Reserva de emergencia cobre imprevistos.",
                java.util.Map.of(
                        "source", "reserva-emergencia.md",
                        "theme", "RESERVA_EMERGENCIA",
                        "audience", "BEGINNER",
                        "language", "pt-BR"));
    }
}
