package com.lrsoftwares.finance_ai_agent.service.sprint8;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.CategoryBudgetResponse;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.CreateCategoryBudgetRequest;
import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.entity.CategoryBudget;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;
import com.lrsoftwares.finance_ai_agent.exception.BusinessException;
import com.lrsoftwares.finance_ai_agent.exception.ResourceNotFoundException;
import com.lrsoftwares.finance_ai_agent.repository.CategoryBudgetRepository;
import com.lrsoftwares.finance_ai_agent.repository.CategoryRepository;
import com.lrsoftwares.finance_ai_agent.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryBudgetService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_ALERT_THRESHOLD = new BigDecimal("0.90");

    private final CategoryBudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public CategoryBudgetResponse createOrUpdate(CreateCategoryBudgetRequest request) {
        UUID userId = authenticatedUserProvider.getUserId();
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria nao encontrada."));

        if (!category.getUserId().equals(userId)) {
            throw new BusinessException("Nao e permitido criar orcamento para categoria de outro usuario.");
        }
        if (category.getType() != TransactionType.EXPENSE) {
            throw new BusinessException("Orcamento mensal por categoria aceita apenas categorias de despesa.");
        }

        LocalDate monthStart = request.month().atDay(1);
        CategoryBudget budget = budgetRepository
                .findByUserIdAndCategoryIdAndMonthStart(userId, request.categoryId(), monthStart)
                .orElseGet(CategoryBudget::new);

        budget.setUserId(userId);
        budget.setCategory(category);
        budget.setMonthStart(monthStart);
        budget.setPlannedAmount(request.plannedAmount());
        budget.setAlertThresholdRate(request.alertThresholdRate() == null
                ? DEFAULT_ALERT_THRESHOLD
                : request.alertThresholdRate());

        return toResponse(budgetRepository.save(budget));
    }

    public List<CategoryBudgetResponse> listByMonth(YearMonth month) {
        UUID userId = authenticatedUserProvider.getUserId();
        LocalDate monthStart = month.atDay(1);

        return budgetRepository.findByUserIdAndMonthStart(userId, monthStart)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void delete(UUID id) {
        UUID userId = authenticatedUserProvider.getUserId();
        CategoryBudget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orcamento de categoria nao encontrado."));

        if (!budget.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Orcamento de categoria nao encontrado.");
        }

        budgetRepository.delete(budget);
    }

    private CategoryBudgetResponse toResponse(CategoryBudget budget) {
        LocalDate start = budget.getMonthStart().withDayOfMonth(1);
        LocalDate end = budget.getMonthStart().withDayOfMonth(budget.getMonthStart().lengthOfMonth());
        BigDecimal spent = transactionRepository.sumExpenseByCategoryAndPeriod(
                budget.getUserId(),
                budget.getCategory().getId(),
                start,
                end);

        BigDecimal remaining = budget.getPlannedAmount().subtract(spent);
        BigDecimal consumedRate = ZERO;
        if (budget.getPlannedAmount().compareTo(ZERO) > 0) {
            consumedRate = spent.divide(budget.getPlannedAmount(), 4, RoundingMode.HALF_UP);
        }

        boolean exceeded = spent.compareTo(budget.getPlannedAmount()) > 0;

        return new CategoryBudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                YearMonth.from(budget.getMonthStart()),
                budget.getPlannedAmount(),
                spent,
                remaining,
                consumedRate,
                budget.getAlertThresholdRate(),
                exceeded);
    }
}
