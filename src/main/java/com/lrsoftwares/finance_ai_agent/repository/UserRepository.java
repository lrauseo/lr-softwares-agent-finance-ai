
package com.lrsoftwares.finance_ai_agent.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lrsoftwares.finance_ai_agent.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCase(String email);
}