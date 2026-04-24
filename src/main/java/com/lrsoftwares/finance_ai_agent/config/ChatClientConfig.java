package com.lrsoftwares.finance_ai_agent.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

@Configuration
public class ChatClientConfig {

	@Bean
	ChatClient openAiChatClient(@NonNull OpenAiChatModel chatModel) {
		return ChatClient.create(chatModel);
	}
	// @Bean
	// ChatClient anthropicChatClient(@NonNull AnthropicChatModel chatModel)
	// {
	// return ChatClient.create(chatModel);
	// }
}
