package com.lrsoftwares.finance_ai_agent.controller;

import java.time.YearMonth;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lrsoftwares.finance_ai_agent.dto.FinancialDiagnosisResponse;
import com.lrsoftwares.finance_ai_agent.service.analysis.FinancialAnalysisService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class FinancialAnalysisController {

    private final FinancialAnalysisService financialAnalysisService;

    @GetMapping("/monthly")
    public FinancialDiagnosisResponse analyzeMonthly(
            @RequestParam UUID userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth monthDate
    ) {
        return financialAnalysisService.analyzeMonthly(userId, monthDate);
    }
}
