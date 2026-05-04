package com.lrsoftwares.finance_ai_agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;

@Configuration
public class ChatClientConfig {

	@Bean
	ChatClient openAiChatClient(@NonNull OpenAiChatModel chatModel) {
		return ChatClient.create(chatModel);
	}

	@Bean
	@Primary
	ChatClient deepseekChatClient(@NonNull DeepSeekChatModel chatModel) {
		return ChatClient.create(chatModel);
	}
}
