package com.lrsoftwares.finance_ai_agent.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lrsoftwares.finance_ai_agent.entity.CategoryBudget;

public interface CategoryBudgetRepository extends JpaRepository<CategoryBudget, UUID> {

    List<CategoryBudget> findByUserIdAndMonthStart(UUID userId, LocalDate monthStart);

    Optional<CategoryBudget> findByUserIdAndCategoryIdAndMonthStart(UUID userId, UUID categoryId, LocalDate monthStart);
}
