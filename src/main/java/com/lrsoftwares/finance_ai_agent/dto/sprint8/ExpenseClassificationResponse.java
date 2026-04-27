package com.lrsoftwares.finance_ai_agent.dto.sprint8;

import java.util.UUID;

public record ExpenseClassificationResponse(
        UUID categoryId,
        String categoryName,
        double confidence,
        String rationale
) {
}
