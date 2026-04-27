package com.lrsoftwares.finance_ai_agent.dto.sprint8;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record McpInvokeRequest(
        @NotBlank String tool,
        @NotNull Map<String, Object> args
) {
}
