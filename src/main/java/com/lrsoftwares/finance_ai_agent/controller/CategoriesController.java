package com.lrsoftwares.finance_ai_agent.controller;

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

import com.lrsoftwares.finance_ai_agent.dto.CategoryResponse;
import com.lrsoftwares.finance_ai_agent.dto.CreateCategoryRequest;
import com.lrsoftwares.finance_ai_agent.dto.UpdateCategoryRequest;
import com.lrsoftwares.finance_ai_agent.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoriesController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse criar(@RequestBody @Valid CreateCategoryRequest request) {
        return categoryService.salvar(request);
    }

    @GetMapping
    public List<CategoryResponse> listarPorUsuario(@RequestParam UUID userId) {
        return categoryService.listarPorUsuario(userId);
    }

    @PutMapping("/{id}")
    public CategoryResponse atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateCategoryRequest request
    ) {
        return categoryService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id) {
        categoryService.deletar(id);
    }
}
