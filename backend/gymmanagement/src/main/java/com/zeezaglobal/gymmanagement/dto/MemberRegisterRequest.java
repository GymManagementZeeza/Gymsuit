package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.Gender;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MemberRegisterRequest(
        @NotBlank @Email String email,
        @NotNull Long gymId,
        @NotNull Long planId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone,
        LocalDate dateOfBirth,
        Gender gender,
        @AssertTrue(message = "You must accept the waiver to register") boolean waiverAccepted
) {
}
