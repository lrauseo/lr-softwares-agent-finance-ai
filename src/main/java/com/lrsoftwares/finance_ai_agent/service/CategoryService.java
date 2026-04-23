package com.lrsoftwares.finance_ai_agent.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

import com.lrsoftwares.finance_ai_agent.dto.CategoryResponse;
import com.lrsoftwares.finance_ai_agent.dto.CreateCategoryRequest;
import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.mapper.CategoryMapper;
import com.lrsoftwares.finance_ai_agent.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
public class CategoryService {
	private final CategoryRepository categoryRepository;
	private final CategoryMapper categoryMapper;

	public void salvar(@NonNull CreateCategoryRequest request) {
		Category category = categoryMapper.toEntity(request);
		category.setSystemDefault(Boolean.FALSE);
		categoryRepository.save(category);
	}

	public List<CategoryResponse> getByUserId(@NonNull UUID userId) {
		List<Category> categories = categoryRepository.findByUserId(userId);
		return categories.stream()
				.filter(Objects::nonNull)
				.map(categoryMapper::toDto)
				.toList();
	}

	public Optional<CategoryResponse> findById(@NonNull UUID categoryId) {
		return categoryRepository.findById(categoryId)
				.map(categoryMapper::toDto);
		
	}
}
