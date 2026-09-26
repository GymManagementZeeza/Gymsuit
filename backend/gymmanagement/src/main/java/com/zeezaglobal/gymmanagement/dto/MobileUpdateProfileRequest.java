package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.NotBlank;

public record MobileUpdateProfileRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        String lastName
) {
}
