package com.lrsoftwares.finance_ai_agent.controller;

import java.time.YearMonth;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lrsoftwares.finance_ai_agent.dto.sprint8.ActionRecommendationResponse;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.CashflowForecastResponse;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.DashboardResponse;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.ExpenseClassificationRequest;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.ExpenseClassificationResponse;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.ProactiveNotificationResponse;
import com.lrsoftwares.finance_ai_agent.service.sprint8.ActionRecommendationService;
import com.lrsoftwares.finance_ai_agent.service.sprint8.CashflowForecastService;
import com.lrsoftwares.finance_ai_agent.service.sprint8.DashboardService;
import com.lrsoftwares.finance_ai_agent.service.sprint8.ExpenseAutoClassificationService;
import com.lrsoftwares.finance_ai_agent.service.sprint8.ProactiveNotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class InsightsController {

    private final DashboardService dashboardService;
    private final CashflowForecastService cashflowForecastService;
    private final ProactiveNotificationService proactiveNotificationService;
    private final ExpenseAutoClassificationService expenseAutoClassificationService;
    private final ActionRecommendationService actionRecommendationService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth monthDate
    ) {
        return dashboardService.load(monthDate);
    }

    @GetMapping("/cashflow-forecast")
    public CashflowForecastResponse forecast(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth baseMonth,
            @RequestParam(defaultValue = "3") int monthsAhead
    ) {
        return cashflowForecastService.forecast(baseMonth, monthsAhead);
    }

    @GetMapping("/notifications")
    public List<ProactiveNotificationResponse> notifications(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth monthDate
    ) {
        return proactiveNotificationService.generate(monthDate);
    }

    @PostMapping("/classify-expense")
    public ExpenseClassificationResponse classifyExpense(@RequestBody @Valid ExpenseClassificationRequest request) {
        return expenseAutoClassificationService.classify(request);
    }

    @GetMapping("/recommendations")
    public ActionRecommendationResponse recommendations(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth monthDate
    ) {
        return actionRecommendationService.generate(monthDate);
    }
}
