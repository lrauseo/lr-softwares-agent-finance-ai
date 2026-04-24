package com.lrsoftwares.finance_ai_agent.service.ai;

import java.util.UUID;

import org.springframework.lang.NonNull;

import com.lrsoftwares.finance_ai_agent.dto.chat.ChatAnswerResponse;
import com.lrsoftwares.finance_ai_agent.dto.chat.ChatQuestionRequest;

public interface FinancialAdvisorChatService {

	ChatAnswerResponse answer(@NonNull UUID sessionId, @NonNull ChatQuestionRequest request);

}