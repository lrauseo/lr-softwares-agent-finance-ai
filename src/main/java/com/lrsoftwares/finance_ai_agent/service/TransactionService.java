package com.lrsoftwares.finance_ai_agent.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.dto.CreateTransactionRequest;
import com.lrsoftwares.finance_ai_agent.dto.TransactionResponse;
import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.exception.BusinessException;
import com.lrsoftwares.finance_ai_agent.exception.ResourceNotFoundException;
import com.lrsoftwares.finance_ai_agent.mapper.TransactionMapper;
import com.lrsoftwares.finance_ai_agent.repository.CategoryRepository;
import com.lrsoftwares.finance_ai_agent.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {
	private final TransactionRepository transactionRepository;
	private final TransactionMapper transactionMapper;
	private final CategoryRepository categoryRepository;
	
	public void salvar(@NonNull CreateTransactionRequest request) {
		Category category = categoryRepository.findById(request.categoryId())
				.orElseThrow(() -> new ResourceNotFoundException("Categoria nao encontrada: " + request.categoryId()));

		if (!category.getUserId().equals(request.userId())) {
			throw new BusinessException("A categoria nao pertence ao usuario informado.");
		}

		if (!category.getType().equals(request.type())) {
			throw new BusinessException("O tipo da categoria difere do tipo da transacao.");
		}

		var transaction = transactionMapper.toEntity(request);
		transaction.setCategory(category);
		transactionRepository.save(transaction);
	}

	public List<TransactionResponse> getByUserAndDate(@NonNull UUID userId, @NonNull LocalDate startDate, @NonNull LocalDate endDate) {
		return transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate)
				.stream()
				.filter(Objects::nonNull)
				.map(transactionMapper::toDto)
				.toList();
	}
	
}
