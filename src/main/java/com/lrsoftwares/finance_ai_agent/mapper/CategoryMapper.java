package com.lrsoftwares.finance_ai_agent.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.lang.NonNull;

import com.lrsoftwares.finance_ai_agent.dto.CategoryResponse;
import com.lrsoftwares.finance_ai_agent.dto.CreateCategoryRequest;
import com.lrsoftwares.finance_ai_agent.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
	
	@NonNull
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "systemDefault", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	Category toEntity(@NonNull CreateCategoryRequest request);

	@NonNull
	CategoryResponse toDto(@NonNull Category category);
}
