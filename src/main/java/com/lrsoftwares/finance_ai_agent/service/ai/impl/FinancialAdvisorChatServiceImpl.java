package com.lrsoftwares.finance_ai_agent.service.ai.impl;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.dto.FinancialDiagnosisResponse;
import com.lrsoftwares.finance_ai_agent.dto.chat.ChatAnswerResponse;
import com.lrsoftwares.finance_ai_agent.dto.chat.ChatQuestionRequest;
import com.lrsoftwares.finance_ai_agent.entity.ChatMessage;
import com.lrsoftwares.finance_ai_agent.entity.ChatRole;
import com.lrsoftwares.finance_ai_agent.service.ChatService;
import com.lrsoftwares.finance_ai_agent.service.ai.FinancialAdvisorChatService;
import com.lrsoftwares.finance_ai_agent.service.ai.LLMClient;
import com.lrsoftwares.finance_ai_agent.service.analysis.FinancialAnalysisService;
import com.lrsoftwares.finance_ai_agent.service.rag.KnowledgeRetrievalService;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class FinancialAdvisorChatServiceImpl implements FinancialAdvisorChatService {

	private static final int MAX_HISTORY_MESSAGES = 6;
	private static final int MAX_KNOWLEDGE_CHUNKS = 3;
	private static final String HISTORY_SUMMARY_MARKER = "[RESUMO_HISTORICO]";

	private final FinancialAnalysisService analysisService;
	private final LLMClient llmClient;
	private final ChatService chatService;
	private final KnowledgeRetrievalService knowledgeRetrievalService;

	@Override
	public ChatAnswerResponse answer(@NonNull UUID sessionId, @NonNull ChatQuestionRequest request) {
		chatService.saveMessage(sessionId, ChatRole.USER, request.question());
		List<ChatMessage> history = chatService.getMessages(sessionId);
		@Nonnull
		YearMonth currentMonth = Objects.requireNonNull(YearMonth.now(), "Não foi possível obter o mês atual");

		FinancialDiagnosisResponse analysis = analysisService.analyzeMonthly(request.userId(), currentMonth);
		List<Document> retrievedChunks = null;
		try {
			retrievedChunks = knowledgeRetrievalService.retrieve(Objects.requireNonNull(request.question()));
		} catch (Exception e) {
			log.error("Falha ao recuperar conhecimento RAG", e);
		}
		String context = buildFullContext(history, analysis, retrievedChunks);

		String userPrompt = Objects.requireNonNull(buildUserPrompt(request.question()),
				"Não foi possível construir o prompt do usuário");
		String systemPrompt = Objects.requireNonNull(buildSystemPrompt(context),
				"Não foi possível construir o prompt do sistema");
		String response = llmClient.generate(systemPrompt, userPrompt);

		if (response == null) {
			response = "Desculpe, não consegui gerar uma resposta no momento.";
		}
		chatService.saveMessage(sessionId, ChatRole.ASSISTANT, response);
		return new ChatAnswerResponse(
				response,
				currentMonth,
				Objects.requireNonNull(LocalDateTime.now(), "Não foi possível obter a data e hora atuais"));
	}

	private String buildFullContext(List<ChatMessage> history, FinancialDiagnosisResponse analysis,
			List<Document> retrievedChunks) {

		StringBuilder sb = new StringBuilder();
		sb.append("Histórico da conversa:\n");
		history.stream()
				.limit(MAX_HISTORY_MESSAGES)
				.forEach(msg -> sb.append(msg.getRole())
						.append(": ")
						.append(toHistoryContent(msg))
						.append("\n"));

		sb.append("\n---\n");
		sb.append(buildContext(analysis));
		sb.append("\n---\n");
		sb.append(buildKnowledgeContext(retrievedChunks));
		return sb.toString();

	}

	private String buildKnowledgeContext(List<Document> chunks) {
		if (chunks == null || chunks.isEmpty()) {
			return "Nenhum conteúdo interno relevante encontrado.";
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Conhecimento interno recuperado:\n");

		chunks.stream().limit(MAX_KNOWLEDGE_CHUNKS).forEach(doc -> {
			var metadata = doc.getMetadata();

			sb.append("- Fonte: ").append(metadata.get("source")).append("\n");
			sb.append("  Tema: ").append(metadata.get("theme")).append("\n");
			sb.append("  Audiência: ").append(metadata.get("audience")).append("\n");
			sb.append("  Idioma: ").append(metadata.get("language")).append("\n");
			sb.append("  Conteúdo: ").append(Objects.toString(doc.getText(), "")).append("\n\n");
		});

		return sb.toString();
	}

	private String toHistoryContent(ChatMessage message) {
		if (message.getRole() != ChatRole.ASSISTANT) {
			return Objects.toString(message.getContent(), "");
		}

		return extractAssistantHistorySummary(Objects.toString(message.getContent(), ""));
	}

	private String extractAssistantHistorySummary(String content) {
		int markerIndex = content.indexOf(HISTORY_SUMMARY_MARKER);
		if (markerIndex < 0) {
			return "Resumo não informado pelo assistente na mensagem anterior.";
		}

		String summary = content.substring(markerIndex + HISTORY_SUMMARY_MARKER.length()).trim();
		return summary.isEmpty() ? "Resumo não informado pelo assistente na mensagem anterior." : summary;
	}

	private String buildContext(FinancialDiagnosisResponse analysis) {
		var summary = analysis.summary();

		StringBuilder sb = new StringBuilder();
		sb.append("Resumo financeiro do mês:\n");
		sb.append("Receita total: ").append(summary.totalIncome()).append("\n");
		sb.append("Despesa total: ").append(summary.totalExpense()).append("\n");
		sb.append("Saldo: ").append(summary.balance()).append("\n\n");

		sb.append("Categorias de despesa:\n");
		summary.categories().forEach(cat -> sb.append("- ").append(cat.category())
				.append(": ").append(cat.total()).append("\n"));

		sb.append("\nAlertas:\n");
		analysis.alerts().forEach(alert -> sb.append("- ").append(alert.title())
				.append(": ").append(alert.message()).append("\n"));

		sb.append("\nDestaques:\n");
		analysis.highlights().forEach(h -> sb.append("- ").append(h).append("\n"));

		return sb.toString();
	}

	private String buildSystemPrompt(String context) {
		return """
				Você é um consultor financeiro pessoal educacional.
				Seu papel é ajudar o usuário a compreender sua situação financeira,
				organizar orçamento, analisar gastos, definir metas e entender opções.
				REGRAS:
				- Nunca invente dados financeiros do usuário.
				- Quando precisar de dados, solicite ou use ferramentas disponíveis.
				- Sempre diferencie fato, cálculo e sugestão.
				- Não forneça aconselhamento regulado de investimento como recomendação definitiva.
				- Priorize educação financeira, clareza e segurança.
				- Quando usar conteúdo recuperado, cite as fontes.
				- Quando faltar informação, declare explicitamente o que está faltando.
				- Use apenas os dados fornecidos.
				- Nunca invente valores.
				- Explique o problema com clareza, identifique causas prováveis e proponha ações práticas e objetivas.
				- Não forneça recomendação regulada de investimento.
				- Use apenas os dados fornecidos
				- Não invente valores
				- Seja direto e prático
				- Não dê recomendações de investimento
				- Foque em economia, controle e organização
				- Responda em pt-BR.
				- Formate sua resposta em Markdown (use títulos, listas, negrito e itálico quando apropriado).
				- Priorize instruções práticas, ordem de decisão, riscos e trade-offs.
				- Se houver conflito entre eficiência matemática e aderência comportamental, explicite o conflito.
				- Quando o tema for dívida, sempre diferencie:
				- reduzir juros
				- reduzir parcela
				- reduzir prazo
				- reduzir risco jurídico
				- Não considerar transferência entre contas de mesma titularidade como receita ou despesa deve ser tratado como transferência interna.
				- Quando o tema for alavancagem, sempre inclua cenário de perda.
				- Ao final de TODA resposta, adicione obrigatoriamente um bloco para histórico exatamente neste formato:
				[RESUMO_HISTORICO]
				- frase 1
				- frase 2
				- frase 3
				- Esse bloco deve ter no máximo 3 bullets curtos e objetivos.
				- Nunca omita o bloco [RESUMO_HISTORICO].

				DADOS DO USUÁRIO:
				%s
				RESPONDA EM PORTUGUÊS BRASILEIRO.
				"""
				.formatted(context);

	}

	private String buildUserPrompt(String question) {
		return """
				PERGUNTA:
				%s

				RESPONDA DE FORMA CLARA E OBJETIVA.
				""".formatted(question);
	}
}