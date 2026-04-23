package com.lrsoftwares.finance_ai_agent.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lrsoftwares.finance_ai_agent.dto.CreateTransactionRequest;
import com.lrsoftwares.finance_ai_agent.dto.TransactionResponse;
import com.lrsoftwares.finance_ai_agent.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Endpoints para gerenciamento de transações")
public class TransactionsController {
	private final TransactionService transactionService;

	@PostMapping("/")
	@Operation(summary = "Criar transação", description = "Cria uma nova transação de receita ou despesa")
	public ResponseEntity<Void> postTransaction(@Valid @RequestBody @NonNull CreateTransactionRequest request) {
		transactionService.salvar(request);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/")
	@Operation(summary = "Listar transações", description = "Lista transações por período de datas para um usuário")
	public ResponseEntity<List<TransactionResponse>> getByUserAndDate(@RequestParam @NonNull UUID userId,
			@RequestParam @NonNull LocalDate startDate,
			@RequestParam @NonNull LocalDate endDate) {
		return ResponseEntity.ok(transactionService.getByUserAndDate(userId, startDate, endDate));
	}

}
