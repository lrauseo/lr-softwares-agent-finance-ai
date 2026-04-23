package com.lrsoftwares.finance_ai_agent.controller;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lrsoftwares.finance_ai_agent.dto.MonthlySummaryResponse;
import com.lrsoftwares.finance_ai_agent.service.SummaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
@Tag(name = "Summary", description = "Endpoints para resumo e análise de transações")
public class SummaryController {
	private final SummaryService summaryService;

	@GetMapping("/monthly")
	@Operation(summary = "Resumo mensal", description = "Retorna o resumo mensal com receitas, despesas e categorias")
	public ResponseEntity<?> getSummaryMonthlyByUserIdAndDate(@RequestParam @NonNull UUID userId,
			@Parameter(
                description = "Mes de referencia no padrao yyyy-MM. Exemplo: 2026-04",
                schema = @Schema(
                    type = "string",
                    pattern = "^[0-9]{4}-(0[1-9]|1[0-2])$",
                    example = "2026-04"
                )
            ) @RequestParam @DateTimeFormat(pattern = "yyyy-MM") @NonNull YearMonth monthDate) {
		LocalDate firstDayOfMonth = monthDate.atDay(1);
		MonthlySummaryResponse response = summaryService.getMonthlySummary(userId, firstDayOfMonth);
		return ResponseEntity.ok(response);
	}

}
