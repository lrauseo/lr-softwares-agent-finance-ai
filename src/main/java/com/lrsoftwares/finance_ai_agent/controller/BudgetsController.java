package com.lrsoftwares.finance_ai_agent.controller;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.lrsoftwares.finance_ai_agent.dto.sprint8.CategoryBudgetResponse;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.CreateCategoryBudgetRequest;
import com.lrsoftwares.finance_ai_agent.service.sprint8.CategoryBudgetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetsController {

    private final CategoryBudgetService categoryBudgetService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryBudgetResponse createOrUpdate(@RequestBody @Valid CreateCategoryBudgetRequest request) {
        return categoryBudgetService.createOrUpdate(request);
    }

    @GetMapping
    public List<CategoryBudgetResponse> list(@RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth monthDate) {
        return categoryBudgetService.listByMonth(monthDate);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        categoryBudgetService.delete(id);
    }
}
