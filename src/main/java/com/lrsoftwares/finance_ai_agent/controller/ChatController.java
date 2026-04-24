package com.lrsoftwares.finance_ai_agent.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lrsoftwares.finance_ai_agent.dto.chat.ChatAnswerResponse;
import com.lrsoftwares.finance_ai_agent.dto.chat.ChatQuestionRequest;
import com.lrsoftwares.finance_ai_agent.service.ai.FinancialAdvisorChatService;
import com.lrsoftwares.finance_ai_agent.service.ai.impl.FinancialAdvisorChatServiceImpl;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
	private final FinancialAdvisorChatService chatService;

	@PostMapping("/question")
	public ChatAnswerResponse ask(@RequestBody @Valid ChatQuestionRequest request) {
		return chatService.answer(request);
	}
}
