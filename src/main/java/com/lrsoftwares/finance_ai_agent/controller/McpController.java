package com.lrsoftwares.finance_ai_agent.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lrsoftwares.finance_ai_agent.dto.sprint8.McpInvokeRequest;
import com.lrsoftwares.finance_ai_agent.dto.sprint8.McpManifestResponse;
import com.lrsoftwares.finance_ai_agent.service.sprint8.McpIntegrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpController {

    private final McpIntegrationService mcpIntegrationService;

    @GetMapping("/manifest")
    public McpManifestResponse manifest() {
        return mcpIntegrationService.manifest();
    }

    @PostMapping("/invoke")
    public Map<String, Object> invoke(@RequestBody @Valid McpInvokeRequest request) {
        Object result = mcpIntegrationService.invoke(request.tool(), request.args());
        return Map.of("tool", request.tool(), "result", result);
    }
}
