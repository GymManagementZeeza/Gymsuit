package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** Self-service sign-up from a gym's QR code: the admin member form plus a plan and a drawn waiver signature. */
public record PublicRegisterRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotBlank String phone,
        LocalDate dateOfBirth,
        Gender gender,
        Double heightCm,
        Double weightKg,
        String bloodGroup,
        String medicalNotes,
        String emergencyContactName,
        String emergencyContactPhone,
        String emergencyContactRelationship,
        @NotNull Boolean waiverAccepted,
        String waiverSignature,
        LocalDate joinDate,
        @NotNull Long planId
) {
}
