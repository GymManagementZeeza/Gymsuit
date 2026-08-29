package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.CompensationRequest;
import com.zeezaglobal.gymmanagement.dto.CompensationResponse;
import com.zeezaglobal.gymmanagement.entity.PayType;
import com.zeezaglobal.gymmanagement.entity.Trainer;
import com.zeezaglobal.gymmanagement.entity.TrainerCompensation;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.TrainerCompensationRepository;
import com.zeezaglobal.gymmanagement.repository.TrainerRepository;
import com.zeezaglobal.gymmanagement.util.CurrencyCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrainerCompensationService {

    private final TrainerCompensationRepository compensationRepository;
    private final TrainerRepository trainerRepository;

    public CompensationResponse get(Long gymId, Long trainerId) {
        getTrainerOrThrow(gymId, trainerId);
        TrainerCompensation compensation = compensationRepository.findByTrainerId(trainerId)
                .orElseThrow(() -> new ResourceNotFoundException("No compensation configured for trainer " + trainerId));
        return CompensationResponse.fromEntity(compensation);
    }

    public CompensationResponse upsert(Long gymId, Long trainerId, CompensationRequest request) {
        Trainer trainer = getTrainerOrThrow(gymId, trainerId);
        validate(request);

        TrainerCompensation compensation = compensationRepository.findByTrainerId(trainerId)
                .orElseGet(() -> {
                    TrainerCompensation created = new TrainerCompensation();
                    created.setTrainer(trainer);
                    return created;
                });

        compensation.setPayType(request.payType());
        compensation.setHourlyRate(request.payType() == PayType.HOURLY ? request.hourlyRate() : null);
        compensation.setMonthlySalary(request.payType() == PayType.SALARY ? request.monthlySalary() : null);
        compensation.setCurrency(CurrencyCodes.validate(request.currency()));

        return CompensationResponse.fromEntity(compensationRepository.save(compensation));
    }

    private void validate(CompensationRequest request) {
        if (request.payType() == PayType.HOURLY && request.hourlyRate() == null) {
            throw new BadRequestException("hourlyRate is required for HOURLY pay type");
        }
        if (request.payType() == PayType.SALARY && request.monthlySalary() == null) {
            throw new BadRequestException("monthlySalary is required for SALARY pay type");
        }
    }

    private Trainer getTrainerOrThrow(Long gymId, Long trainerId) {
        return trainerRepository.findByIdAndGymId(trainerId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found with id: " + trainerId + " in gym: " + gymId));
    }
}
