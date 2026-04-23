package com.lrsoftwares.finance_ai_agent;

import org.springframework.boot.SpringApplication;

public class TestFinanceAiAgentApplication {

	public static void main(String[] args) {
		SpringApplication.from(FinanceAiAgentApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
