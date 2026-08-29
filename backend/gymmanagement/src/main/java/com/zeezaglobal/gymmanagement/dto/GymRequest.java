package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GymRequest(
        @NotBlank String name,
        String addressLine,
        String city,
        String state,
        String postalCode,
        String country,
        String phone,
        @Email String email,
        String logoUrl,
        String upiId
) {
}
