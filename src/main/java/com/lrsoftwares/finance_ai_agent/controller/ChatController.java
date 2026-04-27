package com.lrsoftwares.finance_ai_agent.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lrsoftwares.finance_ai_agent.dto.chat.ChatAnswerResponse;
import com.lrsoftwares.finance_ai_agent.dto.chat.ChatQuestionRequest;
import com.lrsoftwares.finance_ai_agent.entity.ChatMessage;
import com.lrsoftwares.finance_ai_agent.entity.ChatSession;
import com.lrsoftwares.finance_ai_agent.service.ChatService;
import com.lrsoftwares.finance_ai_agent.service.ai.FinancialAdvisorChatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;
	private final FinancialAdvisorChatService advisorService;

	@PostMapping("/sessions")
	public ChatSession createSession(@RequestParam UUID userId) {
		return chatService.createSession();
	}

	@GetMapping("/sessions")
	public List<ChatSession> listSessions(@RequestParam UUID userId) {
		return chatService.listSessions();
	}

	@GetMapping("/sessions/{id}/messages")
	public List<ChatMessage> getMessages(@PathVariable UUID id) {
		return chatService.getMessages(id);
	}

	@PostMapping("/sessions/{id}/question")
	public ChatAnswerResponse ask(
			@NonNull @PathVariable UUID id,
			@NonNull @RequestBody @Valid ChatQuestionRequest request) {
		return advisorService.answer(id, request);
	}
}