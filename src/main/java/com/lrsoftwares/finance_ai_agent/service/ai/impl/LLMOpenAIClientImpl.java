package com.lrsoftwares.finance_ai_agent.service.ai.impl;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.service.ai.LLMClient;

import lombok.RequiredArgsConstructor;

@Service("LLMOpenAIClient")
@RequiredArgsConstructor
public class LLMOpenAIClientImpl implements LLMClient {

	@Qualifier("openAiChatClient")
	private final ChatClient chatClient;

	@Override
	public String generate(@NonNull String promptSystem, @NonNull String promptUser) {

		return chatClient.prompt()
				.system(promptSystem)
				.user(promptUser)
				.call()
				.content();
	}
}
