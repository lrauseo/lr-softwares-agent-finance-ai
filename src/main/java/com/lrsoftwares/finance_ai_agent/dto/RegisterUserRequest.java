package com.lrsoftwares.finance_ai_agent.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
		@NotBlank String name,

		@Email @NotBlank String email,

		@NotBlank @Size(min = 6) String password) {
}