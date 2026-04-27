package com.lrsoftwares.finance_ai_agent.service.sprint8;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.config.security.AuthenticatedUserProvider;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.ExpenseClassificationRequest;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.ExpenseClassificationResponse;
import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;
import com.lrsoftwares.finance_ai_agent.exception.BusinessException;
import com.lrsoftwares.finance_ai_agent.repository.CategoryRepository;
import com.lrsoftwares.finance_ai_agent.repository.TransactionRepository;

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
    private final TransactionRepository transactionRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    public ExpenseClassificationResponse classify(ExpenseClassificationRequest request) {
        UUID userId = authenticatedUserProvider.getUserId();
        return classify(userId, request.description());
    }

    public ExpenseClassificationResponse classify(UUID userId, String description) {
        String normalized = normalize(description);

        List<Category> categories = categoryRepository.findByUserId(userId)
                .stream()
                .filter(category -> category.getType() == TransactionType.EXPENSE)
                .toList();

        if (categories.isEmpty()) {
            throw new BusinessException("Nenhuma categoria de despesa encontrada para classificacao automatica.");
        }

        Map<UUID, Integer> historyScores = buildHistoryScores(userId, normalized);

        Category best = categories.stream()
                .max(Comparator.comparingInt(category -> {
                    int lexical = score(category.getName().toLowerCase(Locale.ROOT), normalized);
                    int historical = historyScores.getOrDefault(category.getId(), 0);
                    return lexical + historical;
                }))
                .orElseThrow(() -> new BusinessException("Nao foi possivel classificar o gasto automaticamente."));

        int lexicalScore = score(best.getName().toLowerCase(Locale.ROOT), normalized);
        int historicalScore = historyScores.getOrDefault(best.getId(), 0);
        int combined = lexicalScore + historicalScore;
        double confidence = Math.min(0.98, Math.max(0.51, combined / 18.0));

        return new ExpenseClassificationResponse(
                best.getId(),
                best.getName(),
                confidence,
                "Classificacao por similaridade textual + frequencia historica de descricoes por categoria.");
    }

    private Map<UUID, Integer> buildHistoryScores(UUID userId, String normalizedDescription) {
        List<Object[]> rows = Objects.requireNonNullElse(
            transactionRepository.expenseDescriptionFrequencyByUser(userId),
            List.of());
        Map<UUID, Integer> scores = new HashMap<>();

        for (Object[] row : rows) {
            if (row == null || row.length < 3 || row[0] == null || row[1] == null || row[2] == null) {
                continue;
            }

            String historicalDescription = row[0].toString();
            UUID categoryId = (UUID) row[1];
            int frequency = ((Number) row[2]).intValue();

            int historicalSignal = historicalSimilarityScore(normalizedDescription, historicalDescription);
            if (historicalSignal > 0) {
                scores.merge(categoryId, historicalSignal * frequency, Integer::sum);
            }
        }

        return scores;
    }

    private int historicalSimilarityScore(String inputDescription, String historicalDescription) {
        String normalizedHistorical = normalize(historicalDescription);

        if (normalizedHistorical.isBlank() || inputDescription.isBlank()) {
            return 0;
        }

        if (inputDescription.equals(normalizedHistorical)) {
            return 12;
        }

        if (inputDescription.contains(normalizedHistorical) || normalizedHistorical.contains(inputDescription)) {
            return 7;
        }

        long overlap = tokenize(inputDescription).stream()
                .filter(tokenize(normalizedHistorical)::contains)
                .count();

        return overlap >= 2 ? 4 : 0;
    }

    private List<String> tokenize(String value) {
        return List.of(value.split("\\s+"))
                .stream()
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toList());
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
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
