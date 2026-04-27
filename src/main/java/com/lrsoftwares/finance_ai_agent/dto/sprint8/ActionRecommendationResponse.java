package com.lrsoftwares.finance_ai_agent.dto.sprint8;

import java.time.LocalDateTime;
import java.util.List;

public record ActionRecommendationResponse(
        LocalDateTime generatedAt,
        List<Item> recommendations
) {
    public record Item(
            String title,
            String explanation,
            double confidence,
            String impact
    ) {
    }
}
