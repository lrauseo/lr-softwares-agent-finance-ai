package com.lrsoftwares.finance_ai_agent.dto.sprint8;

import java.time.LocalDateTime;

import com.lrsoftwares.finance_ai_agent.entity.AlertSeverity;

public record ProactiveNotificationResponse(
        String code,
        AlertSeverity severity,
        String title,
        String message,
        LocalDateTime createdAt
) {
}
