package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NutritionistRegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Enter the 6-digit code") String otp,
        @NotBlank String firstName,
        @NotBlank String lastName
) {
}
