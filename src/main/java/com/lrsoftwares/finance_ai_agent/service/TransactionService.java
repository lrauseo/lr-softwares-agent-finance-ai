package com.lrsoftwares.finance_ai_agent.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.dto.CreateTransactionRequest;
import com.lrsoftwares.finance_ai_agent.dto.TransactionResponse;
import com.lrsoftwares.finance_ai_agent.mapper.TransactionMapper;
import com.lrsoftwares.finance_ai_agent.repository.CategoryRepository;
import com.lrsoftwares.finance_ai_agent.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {
	private final CategoryService categoryService;
	private final TransactionRepository transactionRepository;
	private final TransactionMapper transactionMapper;
	private final CategoryRepository categoryRepository;
	
	public void salvar(@NonNull CreateTransactionRequest request) {		
		var transacao = transactionMapper.toEntity(request);
		categoryRepository.findById(request.categoryId()).ifPresent(transacao::setCategory);
		transactionRepository.save(transacao);		
	}
	public List<TransactionResponse> getByUserAndDate(@NonNull UUID userId, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
		return transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate)
				.stream()
				.filter(Objects::nonNull)
				.map(transactionMapper::toDto)
				.toList();
	}
	
}
