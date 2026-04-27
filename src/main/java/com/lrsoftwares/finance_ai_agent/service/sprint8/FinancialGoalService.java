package com.lrsoftwares.finance_ai_agent.service.sprint8;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.CreateFinancialGoalRequest;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.FinancialGoalResponse;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.UpdateGoalProgressRequest;
import com.lrsoftwares.finance_ai_agent.entity.FinancialGoal;
import com.lrsoftwares.finance_ai_agent.entity.GoalStatus;
import com.lrsoftwares.finance_ai_agent.exception.ResourceNotFoundException;
import com.lrsoftwares.finance_ai_agent.repository.FinancialGoalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinancialGoalService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final FinancialGoalRepository repository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public FinancialGoalResponse create(CreateFinancialGoalRequest request) {
        UUID userId = authenticatedUserProvider.getUserId();

        BigDecimal initialAmount = Objects.requireNonNullElse(request.currentAmount(), ZERO);
        GoalStatus status = initialAmount.compareTo(request.targetAmount()) >= 0 ? GoalStatus.ACHIEVED : GoalStatus.ACTIVE;

        FinancialGoal goal = FinancialGoal.builder()
                .userId(userId)
                .name(request.name().trim())
                .targetAmount(request.targetAmount())
                .currentAmount(initialAmount)
                .targetDate(request.targetDate())
                .status(status)
                .build();

        return toResponse(repository.save(goal));
    }

    public List<FinancialGoalResponse> list() {
        UUID userId = authenticatedUserProvider.getUserId();
        return repository.findByUserIdOrderByTargetDateAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public FinancialGoalResponse updateProgress(UUID goalId, UpdateGoalProgressRequest request) {
        UUID userId = authenticatedUserProvider.getUserId();
        FinancialGoal goal = loadOwnedGoal(goalId, userId);

        goal.setCurrentAmount(request.currentAmount());
        goal.setStatus(request.currentAmount().compareTo(goal.getTargetAmount()) >= 0
                ? GoalStatus.ACHIEVED
                : GoalStatus.ACTIVE);

        return toResponse(repository.save(goal));
    }

    public void delete(UUID goalId) {
        UUID userId = authenticatedUserProvider.getUserId();
        FinancialGoal goal = loadOwnedGoal(goalId, userId);
        repository.delete(goal);
    }

    private FinancialGoal loadOwnedGoal(UUID goalId, UUID userId) {
        FinancialGoal goal = repository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta financeira nao encontrada."));

        if (!goal.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Meta financeira nao encontrada.");
        }

        return goal;
    }

    private FinancialGoalResponse toResponse(FinancialGoal goal) {
        BigDecimal progressRate = ZERO;
        if (goal.getTargetAmount() != null && goal.getTargetAmount().compareTo(ZERO) > 0) {
            progressRate = goal.getCurrentAmount()
                    .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP);
        }

        return new FinancialGoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                progressRate,
                goal.getTargetDate(),
                goal.getStatus());
    }
}
