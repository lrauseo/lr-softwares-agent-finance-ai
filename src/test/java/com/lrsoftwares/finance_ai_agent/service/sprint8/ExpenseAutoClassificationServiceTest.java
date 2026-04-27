package com.lrsoftwares.finance_ai_agent.service.sprint8;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.ExpenseClassificationRequest;
import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;
import com.lrsoftwares.finance_ai_agent.repository.CategoryRepository;
import com.lrsoftwares.finance_ai_agent.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class ExpenseAutoClassificationServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AuthenticatedUserProvider authenticatedUserProvider;

    @InjectMocks
    private ExpenseAutoClassificationService service;

    @Test
    void shouldClassifyByDescriptionKeywords() {
        UUID userId = UUID.randomUUID();

        Category food = Category.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Alimentacao")
                .type(TransactionType.EXPENSE)
                .systemDefault(false)
                .build();

        Category transport = Category.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .name("Transporte")
                .type(TransactionType.EXPENSE)
                .systemDefault(false)
                .build();

        when(authenticatedUserProvider.getUserId()).thenReturn(userId);
        when(categoryRepository.findByUserId(userId)).thenReturn(List.of(food, transport));
        when(transactionRepository.expenseDescriptionFrequencyByUser(userId)).thenReturn(List.of());

        var result = service.classify(new ExpenseClassificationRequest("Compra no supermercado", java.math.BigDecimal.TEN));

        assertThat(result.categoryName()).isEqualTo("Alimentacao");
        assertThat(result.confidence()).isGreaterThan(0.5);
    }

        @Test
        void shouldUseHistoricalFrequencyToDisambiguateCategories() {
        UUID userId = UUID.randomUUID();

        Category health = Category.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .name("Saude")
            .type(TransactionType.EXPENSE)
            .systemDefault(false)
            .build();

        Category leisure = Category.builder()
            .id(UUID.randomUUID())
            .userId(userId)
            .name("Lazer")
            .type(TransactionType.EXPENSE)
            .systemDefault(false)
            .build();

        when(authenticatedUserProvider.getUserId()).thenReturn(userId);
        when(categoryRepository.findByUserId(userId)).thenReturn(List.of(health, leisure));
        when(transactionRepository.expenseDescriptionFrequencyByUser(userId)).thenReturn(List.of(
            new Object[] { "consulta cardiologista", health.getId(), 8L },
            new Object[] { "consulta cinema", leisure.getId(), 1L }));

        var result = service.classify(new ExpenseClassificationRequest("Consulta cardiologista", java.math.BigDecimal.TEN));

        assertThat(result.categoryId()).isEqualTo(health.getId());
        assertThat(result.categoryName()).isEqualTo("Saude");
        assertThat(result.confidence()).isGreaterThan(0.6);
        }
}
