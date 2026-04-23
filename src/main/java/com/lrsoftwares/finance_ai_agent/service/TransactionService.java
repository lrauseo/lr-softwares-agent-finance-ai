package com.lrsoftwares.finance_ai_agent.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.dto.CreateTransactionRequest;
import com.lrsoftwares.finance_ai_agent.dto.TransactionResponse;
import com.lrsoftwares.finance_ai_agent.dto.UpdateTransactionRequest;
import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.entity.Transaction;
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

    public TransactionResponse salvar(CreateTransactionRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));

        validarCategoriaParaTransacao(category, request.userId(), request.type());

        Transaction transaction = transactionMapper.toEntity(request);
        transaction.setCategory(category);

        return transactionMapper.toDto(transactionRepository.save(transaction));
    }

    public List<TransactionResponse> getByUserAndDate(UUID userId, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByUserIdAndDateBetween(userId, startDate, endDate)
                .stream()
                .map(transactionMapper::toDto)
                .toList();
    }

    public TransactionResponse buscarPorId(UUID id) {
        return transactionRepository.findById(id)
                .map(transactionMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada."));
    }

    public TransactionResponse atualizar(UUID id, UpdateTransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada."));

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));

        validarCategoriaParaTransacao(category, transaction.getUserId(), request.type());

        transaction.setCategory(category);
        transaction.setDate(request.date());
        transaction.setAmount(request.amount());
        transaction.setType(request.type());
        transaction.setDescription(request.description());
        transaction.setRecurring(request.recurring());

        return transactionMapper.toDto(transactionRepository.save(transaction));
    }

    public void deletar(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada."));

        transactionRepository.delete(transaction);
    }

    private void validarCategoriaParaTransacao(Category category, UUID userId, com.lrsoftwares.finance_ai_agent.entity.TransactionType type) {
        if (!category.getUserId().equals(userId)) {
            throw new BusinessException("A categoria não pertence ao usuário informado.");
        }

        if (!category.getType().equals(type)) {
            throw new BusinessException("O tipo da categoria difere do tipo da transação.");
        }
    }
}
