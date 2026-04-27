package com.lrsoftwares.finance_ai_agent.service.sprint8;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.ActionRecommendationResponse;
import com.lrsoftwares.finance_ai_agent.service.analysis.FinancialAnalysisService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActionRecommendationService {

    private final FinancialAnalysisService financialAnalysisService;
    private final CategoryBudgetService categoryBudgetService;
    private final FinancialGoalService financialGoalService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ActionRecommendationResponse generate(YearMonth month) {
        var userId = authenticatedUserProvider.getUserId();
        var diagnosis = financialAnalysisService.analyzeMonthly(userId, month);
        var budgets = categoryBudgetService.listByMonth(month);
        var goals = financialGoalService.list();

        List<ActionRecommendationResponse.Item> items = new ArrayList<>();

        if (diagnosis.summary().balance().compareTo(BigDecimal.ZERO) < 0) {
            items.add(new ActionRecommendationResponse.Item(
                    "Plano de corte imediato",
                    "Seu saldo esta negativo. Priorize reduzir despesas variaveis por 30 dias e renegociar gastos fixos.",
                    0.92,
                    "alto"));
        }

        budgets.stream()
                .filter(b -> b.exceeded() || b.consumedRate().compareTo(b.alertThresholdRate()) >= 0)
                .limit(2)
                .forEach(budget -> items.add(new ActionRecommendationResponse.Item(
                        "Ajustar teto da categoria " + budget.categoryName(),
                        "Categoria consumiu acima do limite planejado. Defina teto semanal e acompanhe diariamente.",
                        budget.exceeded() ? 0.88 : 0.77,
                        "medio")));

        goals.stream()
                .filter(goal -> goal.progressRate().compareTo(new BigDecimal("0.40")) < 0)
                .min(Comparator.comparing(g -> g.targetDate()))
                .ifPresent(goal -> items.add(new ActionRecommendationResponse.Item(
                        "Reforcar aporte para meta " + goal.name(),
                        "A meta esta com progresso abaixo do esperado. Automatize um valor fixo semanal para recuperar o ritmo.",
                        0.74,
                        "medio")));

        if (items.isEmpty()) {
            items.add(new ActionRecommendationResponse.Item(
                    "Manter disciplina atual",
                    "Seus indicadores estao estaveis. Mantenha acompanhamento semanal e revise orcamentos no inicio de cada mes.",
                    0.69,
                    "baixo"));
        }

        return new ActionRecommendationResponse(LocalDateTime.now(), items);
    }
}
