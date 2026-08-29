package com.zeezaglobal.gymmanagement.dto;

import com.zeezaglobal.gymmanagement.entity.Trainer;

import java.time.LocalDate;

public record TrainerResponse(
        Long id,
        Long gymId,
        String firstName,
        String lastName,
        String email,
        String phone,
        String specialization,
        String bio,
        LocalDate hireDate,
        String imageUrl,
        String certificateUrl
) {
    public static TrainerResponse fromEntity(Trainer trainer) {
        return new TrainerResponse(
                trainer.getId(),
                trainer.getGym().getId(),
                trainer.getFirstName(),
                trainer.getLastName(),
                trainer.getEmail(),
                trainer.getPhone(),
                trainer.getSpecialization(),
                trainer.getBio(),
                trainer.getHireDate(),
                trainer.getImageUrl(),
                trainer.getCertificateUrl()
        );
    }
}
