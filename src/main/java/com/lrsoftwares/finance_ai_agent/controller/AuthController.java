
package com.lrsoftwares.finance_ai_agent.controller;

import org.springframework.web.bind.annotation.*;

import com.lrsoftwares.finance_ai_agent.dto.AuthRequest;
import com.lrsoftwares.finance_ai_agent.dto.AuthResponse;
import com.lrsoftwares.finance_ai_agent.dto.RegisterUserRequest;
import com.lrsoftwares.finance_ai_agent.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	public AuthResponse register(@RequestBody @Valid RegisterUserRequest request) {
		return new AuthResponse(authService.register(request));
	}

	@PostMapping("/login")
	public AuthResponse login(@RequestBody @Valid AuthRequest request) {
		return new AuthResponse(authService.login(request.email(), request.password()));
	}
}