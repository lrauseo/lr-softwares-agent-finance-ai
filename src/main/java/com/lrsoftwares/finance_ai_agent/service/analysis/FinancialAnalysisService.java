package com.lrsoftwares.finance_ai_agent.service.analysis;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import com.lrsoftwares.finance_ai_agent.dto.FinancialAlert;

public interface FinancialAnalysisService {
    List<FinancialAlert> analyzeMonthlyHealth(UUID userId, YearMonth month);
}