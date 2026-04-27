package com.lrsoftwares.finance_ai_agent.service.sprint8;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.ExpenseClassificationRequest;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.ExpenseClassificationResponse;
import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;
import com.lrsoftwares.finance_ai_agent.exception.BusinessException;
import com.lrsoftwares.finance_ai_agent.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseAutoClassificationService {

    private static final Map<String, String> KEYWORDS = Map.ofEntries(
            Map.entry("mercado", "alimentacao"),
            Map.entry("supermercado", "alimentacao"),
            Map.entry("ifood", "alimentacao"),
            Map.entry("uber", "transporte"),
            Map.entry("99", "transporte"),
            Map.entry("combustivel", "transporte"),
            Map.entry("aluguel", "moradia"),
            Map.entry("energia", "moradia"),
            Map.entry("agua", "moradia"),
            Map.entry("farmacia", "saude"),
            Map.entry("medico", "saude"),
            Map.entry("academia", "saude"),
            Map.entry("netflix", "lazer"),
            Map.entry("cinema", "lazer"));

    private final CategoryRepository categoryRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ExpenseClassificationResponse classify(ExpenseClassificationRequest request) {
        UUID userId = authenticatedUserProvider.getUserId();
        return classify(userId, request.description());
    }

    public ExpenseClassificationResponse classify(UUID userId, String description) {
        String normalized = description.toLowerCase(Locale.ROOT);

        List<Category> categories = categoryRepository.findByUserId(userId)
                .stream()
                .filter(category -> category.getType() == TransactionType.EXPENSE)
                .toList();

        if (categories.isEmpty()) {
            throw new BusinessException("Nenhuma categoria de despesa encontrada para classificacao automatica.");
        }

        Category best = categories.stream()
                .max(Comparator.comparingInt(category -> score(category.getName().toLowerCase(Locale.ROOT), normalized)))
                .orElseThrow(() -> new BusinessException("Nao foi possivel classificar o gasto automaticamente."));

        int score = score(best.getName().toLowerCase(Locale.ROOT), normalized);
        double confidence = Math.min(0.98, Math.max(0.51, score / 10.0));

        return new ExpenseClassificationResponse(
                best.getId(),
                best.getName(),
                confidence,
                "Classificacao por similaridade de descricao com taxonomia de categorias do usuario.");
    }

    private int score(String categoryName, String description) {
        int score = 1;
        if (description.contains(categoryName)) {
            score += 6;
        }

        for (var entry : KEYWORDS.entrySet()) {
            if (description.contains(entry.getKey()) && categoryName.contains(entry.getValue())) {
                score += 4;
            }
        }

        return score;
    }
}
