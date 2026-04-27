package com.lrsoftwares.finance_ai_agent.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lrsoftwares.finance_ai_agent.dto.sprint8.CreateFinancialGoalRequest;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.FinancialGoalResponse;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.UpdateGoalProgressRequest;
import com.lrsoftwares.finance_ai_agent.service.sprint8.FinancialGoalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class FinancialGoalsController {

    private final FinancialGoalService financialGoalService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FinancialGoalResponse create(@RequestBody @Valid CreateFinancialGoalRequest request) {
        return financialGoalService.create(request);
    }

    @GetMapping
    public List<FinancialGoalResponse> list() {
        return financialGoalService.list();
    }

    @PatchMapping("/{id}/progress")
    public FinancialGoalResponse updateProgress(@PathVariable UUID id, @RequestBody @Valid UpdateGoalProgressRequest request) {
        return financialGoalService.updateProgress(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        financialGoalService.delete(id);
    }
}
