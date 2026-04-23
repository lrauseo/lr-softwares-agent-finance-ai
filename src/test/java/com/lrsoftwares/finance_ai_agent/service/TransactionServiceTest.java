package com.lrsoftwares.finance_ai_agent.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lrsoftwares.finance_ai_agent.dto.CreateTransactionRequest;
import com.lrsoftwares.finance_ai_agent.dto.UpdateTransactionRequest;
import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.entity.Transaction;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;
import com.lrsoftwares.finance_ai_agent.exception.BusinessException;
import com.lrsoftwares.finance_ai_agent.exception.ResourceNotFoundException;
import com.lrsoftwares.finance_ai_agent.mapper.TransactionMapper;
import com.lrsoftwares.finance_ai_agent.repository.CategoryRepository;
import com.lrsoftwares.finance_ai_agent.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransactionMapper transactionMapper;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void salvarShouldRejectCategoryFromAnotherUser() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .userId(UUID.randomUUID())
                .type(TransactionType.EXPENSE)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        CreateTransactionRequest request = new CreateTransactionRequest(
                userId,
                categoryId,
                LocalDate.of(2026, 4, 12),
                new BigDecimal("120.00"),
                TransactionType.EXPENSE,
                "Mercado",
                false);

        assertThatThrownBy(() -> transactionService.salvar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A categoria não pertence ao usuário informado.");

        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void salvarShouldRejectCategoryWithDifferentType() {
        UUID userId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .userId(userId)
                .type(TransactionType.INCOME)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        CreateTransactionRequest request = new CreateTransactionRequest(
                userId,
                categoryId,
                LocalDate.of(2026, 4, 12),
                new BigDecimal("120.00"),
                TransactionType.EXPENSE,
                "Mercado",
                false);

        assertThatThrownBy(() -> transactionService.salvar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("O tipo da categoria difere do tipo da transação.");
    }

    @Test
    void atualizarShouldThrowWhenTransactionDoesNotExist() {
        UUID transactionId = UUID.randomUUID();
        UpdateTransactionRequest request = new UpdateTransactionRequest(
                UUID.randomUUID(),
                LocalDate.of(2026, 4, 12),
                new BigDecimal("100.00"),
                TransactionType.EXPENSE,
                "Mercado",
                false);

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.atualizar(transactionId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transação não encontrada.");
    }

    @Test
    void deletarShouldRemoveTransactionWhenItExists() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = Transaction.builder()
                .id(transactionId)
                .userId(UUID.randomUUID())
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        transactionService.deletar(transactionId);

        verify(transactionRepository).delete(transaction);
    }
}
