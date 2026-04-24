package com.lrsoftwares.finance_ai_agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FinanceAiAgentApplicationTests {

	@MockitoBean
	private ChatClient chatClient;

	@Test
	void contextLoads() {
	}

}
