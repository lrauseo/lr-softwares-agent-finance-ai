package com.lrsoftwares.finance_ai_agent.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.dto.CategoryResponse;
import com.lrsoftwares.finance_ai_agent.dto.CreateCategoryRequest;
import com.lrsoftwares.finance_ai_agent.dto.UpdateCategoryRequest;
import com.lrsoftwares.finance_ai_agent.entity.Category;
import com.lrsoftwares.finance_ai_agent.exception.BusinessException;
import com.lrsoftwares.finance_ai_agent.exception.ResourceNotFoundException;
import com.lrsoftwares.finance_ai_agent.mapper.CategoryMapper;
import com.lrsoftwares.finance_ai_agent.repository.CategoryRepository;
import com.lrsoftwares.finance_ai_agent.repository.TransactionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final TransactionRepository transactionRepository;
	private final CategoryMapper categoryMapper;

	public CategoryResponse salvar(CreateCategoryRequest request) {
		boolean exists = categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(
				request.userId(),
				request.name(),
				request.type());

		if (exists) {
			throw new BusinessException("Já existe uma categoria com esse nome e tipo para este usuário.");
		}

		Category category = categoryMapper.toEntity(request);
		category.setSystemDefault(Boolean.FALSE);

		return categoryMapper.toDto(categoryRepository.save(category));
	}

	public List<CategoryResponse> listarPorUsuario(UUID userId) {
		return categoryRepository.findByUserId(userId)
				.stream()
				.map(categoryMapper::toDto)
				.toList();
	}

	public CategoryResponse atualizar(UUID id, UpdateCategoryRequest request) {
		Category category = categoryRepository.findById(Objects.requireNonNull(id))
				.orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));

		boolean exists = categoryRepository.existsByUserIdAndNameIgnoreCaseAndTypeAndIdNot(
				category.getUserId(),
				request.name(),
				request.type(),
				id);

		if (exists) {
			throw new BusinessException("Já existe outra categoria com esse nome e tipo para este usuário.");
		}

		category.setName(request.name());
		category.setType(request.type());

		return categoryMapper.toDto(categoryRepository.save(category));
	}

	public void deletar(UUID id) {
		Category category = categoryRepository.findById(Objects.requireNonNull(id))
				.orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada."));

		if (Boolean.TRUE.equals(category.getSystemDefault())) {
			throw new BusinessException("Categoria padrão do sistema não pode ser removida.");
		}

		if (transactionRepository.existsByCategoryId(id)) {
			throw new BusinessException("Categoria em uso não pode ser removida.");
		}

		categoryRepository.delete(category);
	}
}
