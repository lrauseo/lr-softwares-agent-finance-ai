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

    List<Transaction> findTop200ByUserIdOrderByDateDesc(UUID userId);

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

    @Query("""
            select t.date, coalesce(sum(case when t.type = 'INCOME' then t.amount else 0 end), 0),
                   coalesce(sum(case when t.type = 'EXPENSE' then t.amount else 0 end), 0)
            from Transaction t
            where t.userId = :userId
              and t.date between :start and :end
            group by t.date
            order by t.date asc
            """)
    List<Object[]> dailyCashflow(UUID userId, LocalDate start, LocalDate end);

    @Query("""
            select coalesce(sum(t.amount), 0)
            from Transaction t
            where t.userId = :userId
              and t.type = 'EXPENSE'
              and t.category.id = :categoryId
              and t.date between :start and :end
            """)
    BigDecimal sumExpenseByCategoryAndPeriod(UUID userId, UUID categoryId, LocalDate start, LocalDate end);

    @Query("""
            select lower(trim(t.description)), t.category.id, count(t)
            from Transaction t
            where t.userId = :userId
              and t.type = 'EXPENSE'
              and t.description is not null
              and trim(t.description) <> ''
            group by lower(trim(t.description)), t.category.id
            """)
    List<Object[]> expenseDescriptionFrequencyByUser(UUID userId);
}
