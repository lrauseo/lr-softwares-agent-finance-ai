package com.lrsoftwares.finance_ai_agent.config.security;

import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	@Value("${jwt.secret}")
	private String SECRET;

	public UUID extractUserId(String token) {
		Claims claims = Jwts.parser()
				.verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
				.build()
				.parseSignedClaims(token)
				.getPayload();

		return UUID.fromString(claims.getSubject());
	}

	public String generateToken(UUID userId) {

		return Jwts.builder()
				.subject(userId.toString())
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // 24h				
				.signWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
				.compact();
	}
}