package com.lrsoftwares.finance_ai_agent.dto.sprint8;

import java.util.List;

public record ImportTransactionsResponse(
        int importedCount,
        int skippedCount,
        List<String> warnings
) {
}
