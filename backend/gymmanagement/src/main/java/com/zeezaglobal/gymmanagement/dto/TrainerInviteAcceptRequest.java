package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.Gender;

import java.time.LocalDate;

/**
 * Details for a brand-new member accepting an invite. Left blank when the
 * invitee already has a member record at this gym — accepting then only
 * assigns them to the inviting trainer. The service validates the required
 * fields for the new-member case itself, so this record carries no
 * annotations that would block the existing-member case.
 */
public record TrainerInviteAcceptRequest(
        String firstName,
        String lastName,
        String phone,
        LocalDate dateOfBirth,
        Gender gender,
        Double heightCm,
        Double weightKg,
        Boolean waiverAccepted
) {
}
