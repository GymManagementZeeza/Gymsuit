package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.Gym;

import java.math.BigDecimal;
import java.util.List;

public record GymResponse(
        Long id,
        String name,
        String addressLine,
        String city,
        String state,
        String postalCode,
        String country,
        String phone,
        String email,
        String logoUrl,
        String upiId,
        BigDecimal joiningFee,
        String joiningFeeCurrency,
        ManagerResponse owner,
        List<ManagerResponse> managers
) {
    public static GymResponse fromEntity(Gym gym) {
        return new GymResponse(
                gym.getId(),
                gym.getName(),
                gym.getAddressLine(),
                gym.getCity(),
                gym.getState(),
                gym.getPostalCode(),
                gym.getCountry(),
                gym.getPhone(),
                gym.getEmail(),
                gym.getLogoUrl(),
                gym.getUpiId(),
                gym.getJoiningFee(),
                gym.getJoiningFeeCurrency(),
                gym.getOwner() != null ? ManagerResponse.fromEntity(gym.getOwner()) : null,
                gym.getManagers().stream().map(ManagerResponse::fromEntity).toList()
        );
    }

    /** Same gym profile, with the owner/managers' personal contact details stripped out. */
    public GymResponse withoutStaffContacts() {
        return new GymResponse(
                id, name, addressLine, city, state, postalCode, country, phone, email, logoUrl, upiId,
                joiningFee, joiningFeeCurrency, null, List.of()
        );
    }
}
