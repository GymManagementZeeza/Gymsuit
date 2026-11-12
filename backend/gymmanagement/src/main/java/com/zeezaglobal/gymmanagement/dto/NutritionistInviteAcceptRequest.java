package com.zeezaglobal.gymmanagement.dto;

/**
 * Optional body for accepting a nutritionist invite. Only needed when the invited email has no GymSuit
 * account yet: these details create one (in the Independent gym) before linking it.
 */
public record NutritionistInviteAcceptRequest(
        String firstName,
        String lastName,
        String phone
) {
}
