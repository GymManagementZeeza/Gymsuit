package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MemberRequest(
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
        @NotNull LocalDate joinDate
) {
}
