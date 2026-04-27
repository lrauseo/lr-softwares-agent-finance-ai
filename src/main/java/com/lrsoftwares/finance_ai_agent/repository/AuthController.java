package com.lrsoftwares.finance_ai_agent.repository;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lrsoftwares.finance_ai_agent.dto.AuthRequest;
import com.lrsoftwares.finance_ai_agent.dto.AuthResponse;
import com.lrsoftwares.finance_ai_agent.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody @Valid AuthRequest request) {

        String token = authService.login(request.email(), request.password());

        return new AuthResponse(token);
    }
}