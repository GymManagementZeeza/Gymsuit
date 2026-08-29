package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.NotificationChannel;
import com.zeezaglobal.gymmanagement.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MemberNotifyRequest(
        @NotNull NotificationType type,
        @NotNull NotificationChannel channel,
        @NotBlank String message
) {
}
