package com.lrsoftwares.finance_ai_agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lrsoftwares.finance_ai_agent.dto.CreateCategoryRequest;
import com.lrsoftwares.finance_ai_agent.dto.UpdateCategoryRequest;
import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;
import com.lrsoftwares.finance_ai_agent.exception.BusinessException;
import com.lrsoftwares.finance_ai_agent.exception.ResourceNotFoundException;
import com.lrsoftwares.finance_ai_agent.mapper.CategoryMapper;
import com.lrsoftwares.finance_ai_agent.repository.CategoryRepository;
import com.lrsoftwares.finance_ai_agent.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void salvarShouldRejectDuplicateCategory() {
        UUID userId = UUID.randomUUID();
        CreateCategoryRequest request = new CreateCategoryRequest(userId, "Mercado", TransactionType.EXPENSE);

        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(userId, "Mercado", TransactionType.EXPENSE))
                .thenReturn(true);

        assertThatThrownBy(() -> categoryService.salvar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe uma categoria com esse nome e tipo para este usuário.");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void atualizarShouldRejectDuplicateCategoryForSameUser() {
        UUID categoryId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .userId(userId)
                .name("Mercado")
                .type(TransactionType.EXPENSE)
                .systemDefault(false)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(categoryRepository.existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(
                userId, "Moradia", TransactionType.EXPENSE, categoryId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.atualizar(categoryId, new UpdateCategoryRequest("Moradia", TransactionType.EXPENSE)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Já existe outra categoria com esse nome e tipo para este usuário.");
    }

    @Test
    void deletarShouldRejectSystemDefaultCategory() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .userId(UUID.randomUUID())
                .name("Salario")
                .type(TransactionType.INCOME)
                .systemDefault(true)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));

        assertThatThrownBy(() -> categoryService.deletar(categoryId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Categoria padrão do sistema não pode ser removida.");
    }

    @Test
    void deletarShouldRejectCategoryInUse() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .userId(UUID.randomUUID())
                .name("Mercado")
                .type(TransactionType.EXPENSE)
                .systemDefault(false)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(transactionRepository.existsByCategoryId(categoryId)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deletar(categoryId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Categoria em uso não pode ser removida.");
    }

    @Test
    void deletarShouldRemoveCategoryWhenAllowed() {
        UUID categoryId = UUID.randomUUID();
        Category category = Category.builder()
                .id(categoryId)
                .userId(UUID.randomUUID())
                .name("Mercado")
                .type(TransactionType.EXPENSE)
                .systemDefault(false)
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.of(category));
        when(transactionRepository.existsByCategoryId(categoryId)).thenReturn(false);

        categoryService.deletar(categoryId);

        verify(categoryRepository).delete(category);
    }

    @Test
    void atualizarShouldThrowWhenCategoryDoesNotExist() {
        UUID categoryId = UUID.randomUUID();

        when(categoryRepository.findById(categoryId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> categoryService.atualizar(categoryId, new UpdateCategoryRequest("Teste", TransactionType.EXPENSE)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Categoria não encontrada.");
    }
}
