package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.Gym;

import java.math.BigDecimal;

/** The only gym details a stranger needs to register: name, city, UPI ID and joining fee. */
public record PublicGymResponse(
        Long id,
        String name,
        String city,
        String upiId,
        BigDecimal joiningFee,
        String joiningFeeCurrency
) {
    public static PublicGymResponse fromEntity(Gym gym) {
        return new PublicGymResponse(
                gym.getId(),
                gym.getName(),
                gym.getCity(),
                gym.getUpiId(),
                gym.getJoiningFee(),
                gym.getJoiningFeeCurrency()
        );
    }
}
