package com.lrsoftwares.finance_ai_agent.service.sprint8;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.McpManifestResponse;
import com.lrsoftwares.finance_ai_agent.repository.TransactionRepository;
import com.lrsoftwares.finance_ai_agent.service.SummaryService;
import com.lrsoftwares.finance_ai_agent.service.analysis.FinancialAnalysisService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class McpIntegrationService {

    private final SummaryService summaryService;
    private final FinancialAnalysisService financialAnalysisService;
    private final TransactionRepository transactionRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public McpManifestResponse manifest() {
        return new McpManifestResponse(
                "finance-ai-agent-mcp",
                "1.0.0",
                List.of(
                        new McpManifestResponse.ToolDefinition(
                                "summary.monthly",
                                "Retorna resumo mensal consolidado.",
                                List.of("monthDate")),
                        new McpManifestResponse.ToolDefinition(
                                "analysis.monthly",
                                "Retorna diagnostico financeiro mensal com alertas.",
                                List.of("monthDate")),
                        new McpManifestResponse.ToolDefinition(
                                "transactions.recent",
                                "Retorna ultimas transacoes do usuario.",
                                List.of("days"))));
    }

    public Object invoke(String tool, Map<String, Object> args) {
        var userId = authenticatedUserProvider.getUserId();

        return switch (tool) {
            case "summary.monthly" -> {
                YearMonth month = YearMonth.parse(String.valueOf(args.getOrDefault("monthDate", YearMonth.now())));
                yield summaryService.getSummaryMonthlyByUserIdAndDate(month);
            }
            case "analysis.monthly" -> {
                YearMonth month = YearMonth.parse(String.valueOf(args.getOrDefault("monthDate", YearMonth.now())));
                yield financialAnalysisService.analyzeMonthly(userId, month);
            }
            case "transactions.recent" -> {
                int days = Integer.parseInt(String.valueOf(args.getOrDefault("days", 30)));
                LocalDate end = LocalDate.now();
                LocalDate start = end.minusDays(Math.max(1, days));
                yield transactionRepository.findByUserIdAndDateBetween(userId, start, end).stream()
                        .map(t -> Map.of(
                                "id", t.getId(),
                                "date", t.getDate(),
                                "amount", t.getAmount(),
                                "type", t.getType(),
                                "description", t.getDescription()))
                        .toList();
            }
            default -> throw new IllegalArgumentException("Ferramenta MCP nao suportada: " + tool);
        };
    }
}
