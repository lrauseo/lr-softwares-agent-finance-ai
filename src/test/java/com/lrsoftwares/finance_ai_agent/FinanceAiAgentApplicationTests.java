package com.lrsoftwares.finance_ai_agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import org.springframework.ai.chat.client.ChatClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FinanceAiAgentApplicationTests {

	@MockBean
	private ChatClient chatClient;

	@Test
	void contextLoads() {
	}

}
