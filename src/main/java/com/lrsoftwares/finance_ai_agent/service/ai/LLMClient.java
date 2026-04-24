package com.lrsoftwares.finance_ai_agent.service.ai;

import org.springframework.lang.NonNull;

public interface LLMClient {

	String generate(@NonNull String promptSystem, @NonNull String promptUser);
}
