package com.lrsoftwares.finance_ai_agent.dto;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public record FinancialDiagnosisResponse(
        UUID userId,
        YearMonth month,
        MonthlySummaryResponse summary,
        List<FinancialAlertResponse> alerts,
        List<String> highlights,
        List<String> recommendations
) {}
