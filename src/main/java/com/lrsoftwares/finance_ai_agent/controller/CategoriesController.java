package com.lrsoftwares.finance_ai_agent.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lrsoftwares.finance_ai_agent.dto.CreateCategoryRequest;
import com.lrsoftwares.finance_ai_agent.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Endpoints para gerenciamento de categorias")
public class CategoriesController {
	private final CategoryService categoryService;

	@PostMapping("/")
	@Operation(summary = "Criar categoria", description = "Cria uma nova categoria de transação")
	public ResponseEntity<Void> postCategory(@Valid @RequestBody @NonNull CreateCategoryRequest request) {

		categoryService.salvar(request);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/")
	@Operation(summary = "Listar categorias", description = "Lista todas as categorias do usuário")
	public ResponseEntity<?> getCategoriesByUserId(@RequestParam @NonNull UUID userId) {
		return ResponseEntity.ok(categoryService.getByUserId(userId));
	}

}
