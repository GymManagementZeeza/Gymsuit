package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record TrainerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotBlank String phone,
        String specialization,
        String bio,
        LocalDate hireDate,
        String imageUrl,
        String certificateUrl
) {
}
