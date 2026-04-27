package com.lrsoftwares.finance_ai_agent.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
        @NotBlank String email,
        @NotBlank String password
) {}