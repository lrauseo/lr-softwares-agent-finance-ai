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
    private final SummaryService summaryService;
    private final FinancialAnalysisService analysisService;

    public String chat(@NonNull UUID userId,@NonNull String userMessage) {
        YearMonth currentMonth = YearMonth.now();

        var summary = summaryService.getSummaryMonthlyByUserIdAndDate(userId, currentMonth);
        var alerts = analysisService.analyzeMonthlyHealth(userId, currentMonth);

        String context = """
            Dados financeiros do usuário:
            Resumo mensal: %s
            Alertas: %s
            Pergunta do usuário: %s
            """.formatted(summary, alerts, userMessage);

        return chatClient.prompt()
                .system("""
                    Você é um consultor financeiro pessoal educacional.
                    Use apenas os dados fornecidos.
                    Nunca invente valores.
                    Explique o problema com clareza, identifique causas prováveis
					e proponha ações práticas e objetivas.
					Não forneça recomendação regulada de investimento.
                """)
                .user(context)
                .call()
                .content();
    }
}