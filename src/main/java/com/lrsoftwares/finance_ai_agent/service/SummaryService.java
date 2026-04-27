package com.lrsoftwares.finance_ai_agent.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.dto.CategoryTotalResponse;
import com.lrsoftwares.finance_ai_agent.dto.MonthlySummaryResponse;
import com.lrsoftwares.finance_ai_agent.dto.TransactionResponse;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SummaryService {
	private final TransactionService transactionService;
	private final AuthenticatedUserProvider authenticatedUserProvider;

	public MonthlySummaryResponse getSummaryMonthlyByUserIdAndDate(@NonNull YearMonth monthDate) {
		LocalDate firstDayOfMonth = monthDate.atDay(1);
		return getMonthlySummary(firstDayOfMonth);
	}

	public MonthlySummaryResponse getMonthlySummary(@NonNull LocalDate monthDate) {
		UUID userId = authenticatedUserProvider.getUserId();
		LocalDate startDate = Objects.requireNonNull(monthDate.withDayOfMonth(1));
		LocalDate endDate = Objects.requireNonNull(monthDate.withDayOfMonth(monthDate.lengthOfMonth()));
		var transactions = transactionService.getByUserAndDate(userId, startDate, endDate);
		
		var totalIncome = transactions.stream()
				.filter(transaction -> transaction.type() == TransactionType.INCOME)
				.map(TransactionResponse::amount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		var totalExpense = transactions.stream()
				.filter(transaction -> transaction.type() == TransactionType.EXPENSE)
				.map(TransactionResponse::amount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		var categoryTotals = transactions.stream()
				.filter(transaction -> transaction.type() == TransactionType.EXPENSE)
				.collect(Collectors.groupingBy(TransactionResponse::categoryName,
						Collectors.mapping(TransactionResponse::amount,
								Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))))
				.entrySet().stream()
				.map(entry -> new CategoryTotalResponse(entry.getKey(), entry.getValue()))
				.toList();
		var balance = totalIncome.subtract(totalExpense.abs());

		return new MonthlySummaryResponse(totalIncome, totalExpense, balance, categoryTotals);
	}

}
