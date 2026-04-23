package com.lrsoftwares.finance_ai_agent.mapper;

import org.mapstruct.Mapper;
import org.springframework.lang.NonNull;

import com.lrsoftwares.finance_ai_agent.dto.CreateTransactionRequest;
import com.lrsoftwares.finance_ai_agent.dto.TransactionResponse;
import com.lrsoftwares.finance_ai_agent.entity.Transaction;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
	@NonNull
	Transaction toEntity(@NonNull CreateTransactionRequest request);
	@NonNull
	TransactionResponse toDto(@NonNull Transaction transaction);

}
