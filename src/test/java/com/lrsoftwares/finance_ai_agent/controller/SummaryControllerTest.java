package com.lrsoftwares.finance_ai_agent.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.lrsoftwares.finance_ai_agent.dto.CategoryTotalResponse;
import com.lrsoftwares.finance_ai_agent.dto.MonthlySummaryResponse;
import com.lrsoftwares.finance_ai_agent.service.SummaryService;

@WebMvcTest(SummaryController.class)
class SummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SummaryService summaryService;

    @Test
    void shouldReturnExpectedMonthlySummary() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        YearMonth month = YearMonth.of(2026, 4);

        MonthlySummaryResponse response = new MonthlySummaryResponse(
                new BigDecimal("5000.00"),
                new BigDecimal("800.00"),
                new BigDecimal("4200.00"),
                List.of(new CategoryTotalResponse("Alimentacao", new BigDecimal("800.00"))));

        when(summaryService.getSummaryMonthlyByUserIdAndDate(month)).thenReturn(response);

        mockMvc.perform(get("/api/summary/monthly")
                        .param("userId", userId.toString())
                        .param("monthDate", "2026-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(5000.00))
                .andExpect(jsonPath("$.totalExpense").value(800.00))
                .andExpect(jsonPath("$.balance").value(4200.00))
                .andExpect(jsonPath("$.categories[0].category").value("Alimentacao"))
                .andExpect(jsonPath("$.categories[0].total").value(800.00));
    }
}
