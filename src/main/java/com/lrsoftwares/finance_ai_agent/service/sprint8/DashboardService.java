package com.lrsoftwares.finance_ai_agent.service.sprint8;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.dto.MonthlySummaryResponse;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.DashboardResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final com.lrsoftwares.finance_ai_agent.service.SummaryService summaryService;
    private final CategoryBudgetService categoryBudgetService;
    private final FinancialGoalService financialGoalService;

    public DashboardResponse load(YearMonth month) {
        MonthlySummaryResponse current = summaryService.getSummaryMonthlyByUserIdAndDate(month);
        MonthlySummaryResponse previous = summaryService.getSummaryMonthlyByUserIdAndDate(month.minusMonths(1));

        BigDecimal savingsRate = ZERO;
        if (current.totalIncome().compareTo(ZERO) > 0) {
            savingsRate = current.balance().divide(current.totalIncome(), 4, RoundingMode.HALF_UP);
        }

        BigDecimal expenseGrowthRate = ZERO;
        if (previous.totalExpense().compareTo(ZERO) > 0) {
            expenseGrowthRate = current.totalExpense()
                    .subtract(previous.totalExpense())
                    .divide(previous.totalExpense(), 4, RoundingMode.HALF_UP);
        }

        return new DashboardResponse(
                month,
                current.totalIncome(),
                current.totalExpense(),
                current.balance(),
                savingsRate,
                expenseGrowthRate,
                current.categories().stream()
                        .sorted(Comparator.comparing(com.lrsoftwares.finance_ai_agent.dto.CategoryTotalResponse::total).reversed())
                        .limit(5)
                        .toList(),
                categoryBudgetService.listByMonth(month),
                financialGoalService.list().stream().limit(10).toList());
    }
}
