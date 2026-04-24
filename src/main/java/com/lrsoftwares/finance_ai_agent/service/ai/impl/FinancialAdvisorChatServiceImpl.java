package com.lrsoftwares.finance_ai_agent.service.ai.impl;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.dto.FinancialDiagnosisResponse;
import com.lrsoftwares.finance_ai_agent.dto.chat.ChatAnswerResponse;
import com.lrsoftwares.finance_ai_agent.dto.chat.ChatQuestionRequest;
import com.lrsoftwares.finance_ai_agent.service.ai.FinancialAdvisorChatService;
import com.lrsoftwares.finance_ai_agent.service.ai.LLMClient;
import com.lrsoftwares.finance_ai_agent.service.analysis.FinancialAnalysisService;

import io.micrometer.common.lang.NonNull;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinancialAdvisorChatServiceImpl implements FinancialAdvisorChatService {

	private final FinancialAnalysisService analysisService;
	private final LLMClient llmClient;

	@Override
	public ChatAnswerResponse answer(@NonNull ChatQuestionRequest request) {
		@Nonnull
		YearMonth currentMonth = Objects.requireNonNull(YearMonth.now(), "Não foi possível obter o mês atual");

		FinancialDiagnosisResponse analysis = analysisService.analyzeMonthly(request.userId(), currentMonth);

		String context = buildContext(analysis);

		String userPrompt = Objects.requireNonNull(buildUserPrompt(request.question()),
				"Não foi possível construir o prompt do usuário");
		String systemPrompt = Objects.requireNonNull(buildSystemPrompt(context),
				"Não foi possível construir o prompt do sistema");
		String response = llmClient.generate(systemPrompt, userPrompt);
		if (response == null) {
			response = "Desculpe, não consegui gerar uma resposta no momento.";
		}
		return new ChatAnswerResponse(
				response,
				currentMonth,
				Objects.requireNonNull(LocalDateTime.now(), "Não foi possível obter a data e hora atuais"));
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

				DADOS DO USUÁRIO:
				%s

				""".formatted(context);

	}

	private String buildUserPrompt(String question) {
		return """
				PERGUNTA:
				%s

				RESPONDA DE FORMA CLARA E OBJETIVA.
				""".formatted(question);
	}
}