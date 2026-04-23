package com.lrsoftwares.finance_ai_agent.service.ai;

import java.time.YearMonth;
import java.util.UUID;

import org.mapstruct.control.NoComplexMapping;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.service.SummaryService;
import com.lrsoftwares.finance_ai_agent.service.analysis.FinancialAnalysisService;

import io.micrometer.common.lang.NonNull;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinancialAdvisorChatService {

    private final ChatClient chatClient;
    private final FinancialAnalysisService analysisService;

    public String chat(@NonNull UUID userId,@NonNull String userMessage) {
        YearMonth currentMonth = YearMonth.now();

        var diagnosis = analysisService.analyzeMonthly(userId, currentMonth);

        String context = """
            Dados financeiros do usuário:
            Resumo mensal: %s
            Alertas: %s
            Destaques: %s
            Pergunta do usuário: %s
            """.formatted(diagnosis.summary(), diagnosis.alerts(), diagnosis.highlights(), userMessage);

        return chatClient.prompt()
                .system("""
                    Você é um consultor financeiro pessoal educacional.
					Seu papel é ajudar o usuário a compreender sua situação financeira,
					organizar orçamento, analisar gastos, definir metas e entender opções.

					Regras:
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
                """)
                .user(context)
                .call()
                .content();
    }
}