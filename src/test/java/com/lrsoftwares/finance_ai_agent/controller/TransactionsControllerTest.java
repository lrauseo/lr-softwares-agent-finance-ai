package com.lrsoftwares.finance_ai_agent.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lrsoftwares.finance_ai_agent.dto.TransactionResponse;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;
import com.lrsoftwares.finance_ai_agent.service.TransactionService;

@WebMvcTest(TransactionsController.class)
class TransactionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionService transactionService;

    @Test
    void shouldCreateIncomeTransaction() throws Exception {
        UUID transactionId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID categoryId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        when(transactionService.salvar(any())).thenReturn(new TransactionResponse(
                transactionId,
                userId,
                categoryId,
                "Salario",
                LocalDate.of(2026, 4, 10),
                new BigDecimal("5000.00"),
                TransactionType.INCOME,
                "Salario mensal",
                true));

        String payload = """
                {
                  "userId": "11111111-1111-1111-1111-111111111111",
                  "categoryId": "22222222-2222-2222-2222-222222222222",
                  "date": "2026-04-10",
                  "amount": 5000.00,
                  "type": "INCOME",
                  "description": "Salario mensal",
                  "recurring": true
                }
                """;

        mockMvc.perform(post("/api/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.categoryName").value("Salario"));

        verify(transactionService).salvar(any());
    }

    @Test
    void shouldListTransactionsByPeriod() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(transactionService.getByUserAndDate(userId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)))
                .thenReturn(List.of(new TransactionResponse(
                        UUID.randomUUID(),
                        userId,
                        UUID.randomUUID(),
                        "Mercado",
                        LocalDate.of(2026, 4, 12),
                        new BigDecimal("800.00"),
                        TransactionType.EXPENSE,
                        "Mercado",
                        false)));

        mockMvc.perform(get("/api/transactions")
                        .param("userId", userId.toString())
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"));

        verify(transactionService).getByUserAndDate(userId, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
    }

    @Test
    void shouldGetTransactionById() throws Exception {
        UUID transactionId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID categoryId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        when(transactionService.buscarPorId(transactionId)).thenReturn(new TransactionResponse(
                transactionId,
                userId,
                categoryId,
                "Mercado",
                LocalDate.of(2026, 4, 12),
                new BigDecimal("800.00"),
                TransactionType.EXPENSE,
                "Mercado",
                false));

        mockMvc.perform(get("/api/transactions/{id}", transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transactionId.toString()))
                .andExpect(jsonPath("$.categoryName").value("Mercado"));

        verify(transactionService).buscarPorId(transactionId);
    }

    @Test
    void shouldUpdateTransaction() throws Exception {
        UUID transactionId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID categoryId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        when(transactionService.atualizar(eq(transactionId), any())).thenReturn(new TransactionResponse(
                transactionId,
                userId,
                categoryId,
                "Mercado",
                LocalDate.of(2026, 4, 15),
                new BigDecimal("950.00"),
                TransactionType.EXPENSE,
                "Compra do mes",
                false));

        String payload = """
                {
                  "categoryId": "33333333-3333-3333-3333-333333333333",
                  "date": "2026-04-15",
                  "amount": 950.00,
                  "type": "EXPENSE",
                  "description": "Compra do mes",
                  "recurring": false
                }
                """;

        mockMvc.perform(put("/api/transactions/{id}", transactionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(950.00));

        verify(transactionService).atualizar(eq(transactionId), any());
    }

    @Test
    void shouldDeleteTransaction() throws Exception {
        UUID transactionId = UUID.fromString("99999999-9999-9999-9999-999999999999");

        mockMvc.perform(delete("/api/transactions/{id}", transactionId))
                .andExpect(status().isNoContent());

        verify(transactionService).deletar(transactionId);
    }
}
