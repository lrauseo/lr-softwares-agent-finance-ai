package com.lrsoftwares.finance_ai_agent.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.lrsoftwares.finance_ai_agent.entity.Transaction;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserIdAndDateBetween(UUID userId, LocalDate start, LocalDate end);

    boolean existsByCategoryId(UUID categoryId);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.userId = :userId
              and t.type = :type
              and t.date between :start and :end
            """)
    BigDecimal sumByTypeAndPeriod(UUID userId, TransactionType type, LocalDate start, LocalDate end);

    @Query("""
            select t.category.name, coalesce(sum(t.amount), 0)
            from Transaction t
            where t.userId = :userId
              and t.type = 'EXPENSE'
              and t.date between :start and :end
            group by t.category.name
            order by coalesce(sum(t.amount), 0) desc
            """)
    List<Object[]> summarizeExpensesByCategory(UUID userId, LocalDate start, LocalDate end);
}
