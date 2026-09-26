package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record MobileSendOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        String email,

        // Optional mode: "login" or "register". Defaults to "login" if omitted.
        String mode
) {
}
