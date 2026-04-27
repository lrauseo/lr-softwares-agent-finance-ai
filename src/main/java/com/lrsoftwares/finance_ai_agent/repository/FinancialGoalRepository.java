package com.lrsoftwares.finance_ai_agent.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lrsoftwares.finance_ai_agent.entity.FinancialGoal;

public interface FinancialGoalRepository extends JpaRepository<FinancialGoal, UUID> {

    List<FinancialGoal> findByUserIdOrderByTargetDateAsc(UUID userId);
}
