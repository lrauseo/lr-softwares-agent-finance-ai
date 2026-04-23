package com.lrsoftwares.finance_ai_agent.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.lang.NonNull;

import com.lrsoftwares.finance_ai_agent.dto.CreateTransactionRequest;
import com.lrsoftwares.finance_ai_agent.dto.TransactionResponse;
import com.lrsoftwares.finance_ai_agent.entity.Transaction;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
	@NonNull
	@Mapping(target = "id", ignore = true)
	@Mapping(target = "category", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	Transaction toEntity(@NonNull CreateTransactionRequest request);

	@NonNull
	@Mapping(target = "categoryId", source = "category.id")
	@Mapping(target = "categoryName", source = "category.name")
	TransactionResponse toDto(@NonNull Transaction transaction);

}
