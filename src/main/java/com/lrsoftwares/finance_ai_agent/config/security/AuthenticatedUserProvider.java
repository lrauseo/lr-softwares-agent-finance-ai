package com.lrsoftwares.finance_ai_agent.config.security;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticatedUserProvider {

	public UUID getUserId() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		return (UUID) auth.getPrincipal();
	}
}