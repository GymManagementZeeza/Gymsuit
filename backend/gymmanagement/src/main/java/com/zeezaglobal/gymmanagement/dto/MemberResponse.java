package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.Gender;
import com.zeezaglobal.gymmanagement.entity.Member;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        Long gymId,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        Gender gender,
        Double heightCm,
        Double weightKg,
        Double goalWeightKg,
        LocalDate goalTargetDate,
        String bloodGroup,
        String medicalNotes,
        String emergencyContactName,
        String emergencyContactPhone,
        String emergencyContactRelationship,
        boolean waiverAccepted,
        LocalDate joinDate,
        boolean joiningFeePaid,
        LocalDateTime joiningFeePaidAt,
        Long trainerId,
        String trainerFirstName,
        String trainerLastName
) {
    public static MemberResponse fromEntity(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getGym().getId(),
                member.getFirstName(),
                member.getLastName(),
                member.getEmail(),
                member.getPhone(),
                member.getDateOfBirth(),
                member.getGender(),
                member.getHeightCm(),
                member.getWeightKg(),
                member.getGoalWeightKg(),
                member.getGoalTargetDate(),
                member.getBloodGroup(),
                member.getMedicalNotes(),
                member.getEmergencyContactName(),
                member.getEmergencyContactPhone(),
                member.getEmergencyContactRelationship(),
                member.isWaiverAccepted(),
                member.getJoinDate(),
                member.isJoiningFeePaid(),
                member.getJoiningFeePaidAt(),
                member.getTrainer() != null ? member.getTrainer().getId() : null,
                member.getTrainer() != null ? member.getTrainer().getFirstName() : null,
                member.getTrainer() != null ? member.getTrainer().getLastName() : null
        );
    }
}
