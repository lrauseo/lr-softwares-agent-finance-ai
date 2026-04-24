package com.lrsoftwares.finance_ai_agent.service.ai;

import com.lrsoftwares.finance_ai_agent.dto.chat.ChatAnswerResponse;
import com.lrsoftwares.finance_ai_agent.dto.chat.ChatQuestionRequest;

public interface FinancialAdvisorChatService {

	ChatAnswerResponse answer(ChatQuestionRequest request);

}