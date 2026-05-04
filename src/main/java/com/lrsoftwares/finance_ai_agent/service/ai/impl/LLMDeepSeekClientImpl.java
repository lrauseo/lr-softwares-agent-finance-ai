package com.lrsoftwares.finance_ai_agent.service.ai.impl;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.service.ai.LLMClient;

import lombok.RequiredArgsConstructor;

@Service("LLMDeepSeekClient")
@RequiredArgsConstructor
public class LLMDeepSeekClientImpl implements LLMClient {

	@Qualifier("deepseekChatClient")
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
