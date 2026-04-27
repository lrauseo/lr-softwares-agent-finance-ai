package com.lrsoftwares.finance_ai_agent.service.sprint8;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.dto.FinancialAlertResponse;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.ProactiveNotificationResponse;
import com.lrsoftwares.finance_ai_agent.entity.AlertSeverity;
import com.lrsoftwares.finance_ai_agent.entity.GoalStatus;
import com.lrsoftwares.finance_ai_agent.service.analysis.FinancialAnalysisService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProactiveNotificationService {

    private final FinancialAnalysisService financialAnalysisService;
    private final CategoryBudgetService categoryBudgetService;
    private final FinancialGoalService financialGoalService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public List<ProactiveNotificationResponse> generate(YearMonth month) {
        var now = LocalDateTime.now();
        var userId = authenticatedUserProvider.getUserId();

        List<ProactiveNotificationResponse> notifications = new ArrayList<>();

        var diagnosis = financialAnalysisService.analyzeMonthly(userId, month);
        for (FinancialAlertResponse alert : diagnosis.alerts()) {
            notifications.add(new ProactiveNotificationResponse(
                    "ANALYSIS_" + alert.code(),
                    alert.severity(),
                    alert.title(),
                    alert.message(),
                    now));
        }

        for (var budget : categoryBudgetService.listByMonth(month)) {
            if (budget.exceeded()) {
                notifications.add(new ProactiveNotificationResponse(
                        "BUDGET_EXCEEDED_" + budget.categoryId(),
                        AlertSeverity.WARNING,
                        "Orcamento estourado",
                        "A categoria " + budget.categoryName() + " ultrapassou o limite mensal planejado.",
                        now));
            } else if (budget.consumedRate().compareTo(budget.alertThresholdRate()) >= 0) {
                notifications.add(new ProactiveNotificationResponse(
                        "BUDGET_NEAR_LIMIT_" + budget.categoryId(),
                        AlertSeverity.INFO,
                        "Orcamento proximo do limite",
                        "A categoria " + budget.categoryName() + " ja consumiu "
                                + budget.consumedRate().multiply(new BigDecimal("100")).setScale(0)
                                + "% do orcamento.",
                        now));
            }
        }

        LocalDate today = LocalDate.now();
        for (var goal : financialGoalService.list()) {
            if (goal.status() == GoalStatus.ACHIEVED) {
                continue;
            }
            if (goal.targetDate().isBefore(today)) {
                notifications.add(new ProactiveNotificationResponse(
                        "GOAL_OVERDUE_" + goal.id(),
                        AlertSeverity.WARNING,
                        "Meta atrasada",
                        "A meta " + goal.name() + " passou da data alvo sem ser concluida.",
                        now));
            }
        }

        return notifications;
    }
}
