package com.zeezaglobal.gymmanagement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

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
        String upiId,
        @DecimalMin(value = "0.0") BigDecimal joiningFee,
        @Size(min = 3, max = 3) String joiningFeeCurrency
) {
}
