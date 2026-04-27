package com.lrsoftwares.finance_ai_agent.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.lrsoftwares.finance_ai_agent.config.security.JwtService;
import com.lrsoftwares.finance_ai_agent.dto.RegisterUserRequest;
import com.lrsoftwares.finance_ai_agent.entity.User;
import com.lrsoftwares.finance_ai_agent.exception.BusinessException;
import com.lrsoftwares.finance_ai_agent.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public String register(RegisterUserRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessException("Já existe usuário cadastrado com este e-mail.");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setActive(Boolean.TRUE);

        User saved = userRepository.save(user);

        return jwtService.generateToken(saved.getId());
    }

    public String login(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException("E-mail ou senha inválidos."));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new BusinessException("Usuário inativo.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("E-mail ou senha inválidos.");
        }

        return jwtService.generateToken(user.getId());
    }
}