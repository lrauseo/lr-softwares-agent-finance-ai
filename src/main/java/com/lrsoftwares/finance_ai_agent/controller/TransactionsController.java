package com.lrsoftwares.finance_ai_agent.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lrsoftwares.finance_ai_agent.dto.CreateTransactionRequest;
import com.lrsoftwares.finance_ai_agent.dto.TransactionResponse;
import com.lrsoftwares.finance_ai_agent.dto.UpdateTransactionRequest;
import com.lrsoftwares.finance_ai_agent.service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionsController {

    private final TransactionService transactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse criar(@RequestBody @Valid CreateTransactionRequest request) {
        return transactionService.salvar(request);
    }

    @GetMapping
    public List<TransactionResponse> listarPorUsuarioEPeriodo(
            @RequestParam UUID userId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate
    ) {
        return transactionService.getByUserAndDate(userId, startDate, endDate);
    }

    @GetMapping("/{id}")
    public TransactionResponse buscarPorId(@PathVariable UUID id) {
        return transactionService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public TransactionResponse atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateTransactionRequest request
    ) {
        return transactionService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        transactionService.deletar(id);
    }
}
