package com.lrsoftwares.finance_ai_agent.dto.sprint8;

import java.util.List;

public record McpManifestResponse(
        String server,
        String version,
        List<ToolDefinition> tools
) {
    public record ToolDefinition(
            String name,
            String description,
            List<String> requiredArgs
    ) {
    }
}
