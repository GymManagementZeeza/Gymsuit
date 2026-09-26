package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MobileForgotPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        String email
) {
}
