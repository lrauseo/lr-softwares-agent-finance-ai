package com.lrsoftwares.finance_ai_agent.service.analysis;

import java.time.YearMonth;
import java.util.UUID;

import com.lrsoftwares.finance_ai_agent.dto.FinancialDiagnosisResponse;

public interface FinancialAnalysisService {
    FinancialDiagnosisResponse analyzeMonthly(UUID userId, YearMonth month);
}