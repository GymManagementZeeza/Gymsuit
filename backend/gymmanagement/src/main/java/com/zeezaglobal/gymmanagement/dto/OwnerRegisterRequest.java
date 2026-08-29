package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record OwnerRegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String gymName,
        String addressLine,
        String city,
        String state,
        String postalCode,
        String country,
        String gymPhone,
        @NotBlank String firstName,
        @NotBlank String lastName,
        String phone
) {
}
