package com.lrsoftwares.finance_ai_agent.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lrsoftwares.finance_ai_agent.dto.CategoryResponse;
import com.lrsoftwares.finance_ai_agent.entity.TransactionType;
import com.lrsoftwares.finance_ai_agent.service.CategoryService;

@WebMvcTest(CategoriesController.class)
class CategoriesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void shouldCreateExpenseCategory() throws Exception {
        UUID categoryId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(categoryService.salvar(any())).thenReturn(new CategoryResponse(
                categoryId,
                userId,
                "Alimentação",
                TransactionType.EXPENSE,
                false));

        String payload = """
                {
                  "userId": "11111111-1111-1111-1111-111111111111",
                                                                        "name": "Alimentação",
                  "type": "EXPENSE"
                }
                """;

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.name").value("Alimentação"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.systemDefault").value(false));

        verify(categoryService).salvar(any());
    }

    @Test
    void shouldListCategoriesByUserId() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

                when(categoryService.listarPorUsuario()).thenReturn(List.of(
                new CategoryResponse(UUID.randomUUID(), userId, "Salario", TransactionType.INCOME, false),
                new CategoryResponse(UUID.randomUUID(), userId, "Mercado", TransactionType.EXPENSE, false)));

        mockMvc.perform(get("/api/categories")
                        .param("userId", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

                verify(categoryService).listarPorUsuario();
    }

    @Test
    void shouldUpdateCategory() throws Exception {
        UUID categoryId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        when(categoryService.atualizar(eq(categoryId), any())).thenReturn(new CategoryResponse(
                categoryId,
                userId,
                "Mercado",
                TransactionType.EXPENSE,
                false));

        String payload = """
                {
                                                                        "name": "Mercado",
                  "type": "EXPENSE"
                }
                """;

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mercado"));

        verify(categoryService).atualizar(eq(categoryId), any());
    }

    @Test
    void shouldDeleteCategory() throws Exception {
        UUID categoryId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        mockMvc.perform(delete("/api/categories/{id}", categoryId))
                .andExpect(status().isNoContent());

        verify(categoryService).deletar(categoryId);
    }
}
