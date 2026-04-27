package com.lrsoftwares.finance_ai_agent.dto.sprint8;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import com.lrsoftwares.finance_ai_agent.dto.CategoryTotalResponse;

public record DashboardResponse(
        YearMonth month,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        BigDecimal savingsRate,
        BigDecimal expenseGrowthRate,
        List<CategoryTotalResponse> topExpenseCategories,
        List<CategoryBudgetResponse> budgets,
        List<FinancialGoalResponse> goals
) {
}
