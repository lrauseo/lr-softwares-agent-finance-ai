package com.lrsoftwares.finance_ai_agent.service.analysis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.dto.CategoryTotalResponse;
import com.lrsoftwares.finance_ai_agent.dto.FinancialAlert;
import com.lrsoftwares.finance_ai_agent.dto.FinancialAlertCode;
import com.lrsoftwares.finance_ai_agent.dto.FinancialAlertMessage;
import com.lrsoftwares.finance_ai_agent.dto.FinancialAlertSeverity;
import com.lrsoftwares.finance_ai_agent.dto.MonthlySummaryResponse;
import com.lrsoftwares.finance_ai_agent.service.SummaryService;
import com.lrsoftwares.finance_ai_agent.service.TransactionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinancialAnalysisServiceImpl implements FinancialAnalysisService {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TIGHT_BALANCE_THRESHOLD = BigDecimal.valueOf(10);
    private static final BigDecimal HIGH_CATEGORY_INCOME_PERCENT = BigDecimal.valueOf(35);
    private static final BigDecimal STRONG_GROWTH_PERCENT = BigDecimal.valueOf(40);
    private static final BigDecimal HEAVY_SUBSCRIPTIONS_PERCENT = BigDecimal.valueOf(20);

    private final SummaryService summaryService;
    private final TransactionService transactionService;

    @Override
    public List<FinancialAlert> analyzeMonthlyHealth(@NonNull UUID userId, @NonNull YearMonth month) {
        MonthlySummaryResponse currentMonthSummary = summaryService.getSummaryMonthlyByUserIdAndDate(userId, month);
        MonthlySummaryResponse previousMonthSummary = summaryService.getSummaryMonthlyByUserIdAndDate(userId, month.minusMonths(1));

        List<FinancialAlert> alerts = new ArrayList<>();
        BigDecimal income = safe(currentMonthSummary.totalIncome());
        BigDecimal expense = safe(currentMonthSummary.totalExpense()).abs();
        BigDecimal balance = safe(currentMonthSummary.balance());

        if (expense.compareTo(income) > 0) {
            alerts.add(createAlert(
                    FinancialAlertCode.EXPENSES_EXCEED_INCOME,
                    FinancialAlertMessage.EXPENSES_EXCEED_INCOME.format(),
                    FinancialAlertSeverity.CRITICAL));
        }

        if (income.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal balancePercent = toPercent(balance, income);
            if (balancePercent.compareTo(TIGHT_BALANCE_THRESHOLD) <= 0) {
                alerts.add(createAlert(
                        FinancialAlertCode.TIGHT_BALANCE,
                        FinancialAlertMessage.TIGHT_BALANCE.format(),
                        FinancialAlertSeverity.WARNING));
            }

            evaluateHighCategoryConsumption(currentMonthSummary.categories(), income, alerts);
            evaluateStrongCategoryGrowth(currentMonthSummary.categories(), previousMonthSummary.categories(), alerts);
            evaluateRecurringSubscriptions(userId, month, income, alerts);
        }

        return alerts;
    }

    private void evaluateHighCategoryConsumption(List<CategoryTotalResponse> categories, BigDecimal income,
            List<FinancialAlert> alerts) {
        for (CategoryTotalResponse category : categories) {
            BigDecimal total = safe(category.total());
            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal percent = toPercent(total, income);
            if (percent.compareTo(HIGH_CATEGORY_INCOME_PERCENT) >= 0) {
                alerts.add(createAlert(
                        FinancialAlertCode.HIGH_CATEGORY_CONSUMPTION,
                        FinancialAlertMessage.HIGH_CATEGORY_CONSUMPTION.format(category.category(), percent),
                        FinancialAlertSeverity.WARNING));
            }
        }
    }

    private void evaluateStrongCategoryGrowth(List<CategoryTotalResponse> currentCategories,
            List<CategoryTotalResponse> previousCategories, List<FinancialAlert> alerts) {
        Map<String, BigDecimal> previousByCategory = previousCategories.stream()
                .collect(Collectors.toMap(CategoryTotalResponse::category,
                        category -> safe(category.total()),
                        BigDecimal::add));

        for (CategoryTotalResponse currentCategory : currentCategories) {
            BigDecimal currentTotal = safe(currentCategory.total());
            BigDecimal previousTotal = previousByCategory.getOrDefault(currentCategory.category(), BigDecimal.ZERO);

            if (currentTotal.compareTo(BigDecimal.ZERO) <= 0 || previousTotal.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal growthPercent = currentTotal.subtract(previousTotal)
                    .multiply(HUNDRED)
                    .divide(previousTotal, 2, RoundingMode.HALF_UP);

            if (growthPercent.compareTo(STRONG_GROWTH_PERCENT) >= 0) {
                alerts.add(createAlert(
                        FinancialAlertCode.STRONG_CATEGORY_GROWTH,
                        FinancialAlertMessage.STRONG_CATEGORY_GROWTH
                                .format(currentCategory.category(), growthPercent),
                        FinancialAlertSeverity.WARNING));
            }
        }
    }

    private void evaluateRecurringSubscriptions(UUID userId, YearMonth month, BigDecimal income,
            List<FinancialAlert> alerts) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        BigDecimal recurringExpenses = transactionService.getByUserAndDate(userId, startDate, endDate)
                .stream()
                .filter(transaction -> Boolean.TRUE.equals(transaction.recurring()))
                .map(transaction -> safe(transaction.amount()))
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (recurringExpenses.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal recurringPercent = toPercent(recurringExpenses, income);
        if (recurringPercent.compareTo(HEAVY_SUBSCRIPTIONS_PERCENT) >= 0) {
            alerts.add(createAlert(
                    FinancialAlertCode.HEAVY_RECURRING_SUBSCRIPTIONS,
                    FinancialAlertMessage.HEAVY_RECURRING_SUBSCRIPTIONS.format(recurringPercent),
                    FinancialAlertSeverity.WARNING));
        }
    }

    private FinancialAlert createAlert(FinancialAlertCode code, String message, FinancialAlertSeverity severity) {
        return new FinancialAlert(code.getCode(), message, severity.name());
    }

    private BigDecimal toPercent(BigDecimal value, BigDecimal base) {
        return value
                .multiply(HUNDRED)
                .divide(base, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
