package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NutritionistInviteRequest(
        @NotBlank @Email String email,
        String firstName,
        String lastName
) {
}
