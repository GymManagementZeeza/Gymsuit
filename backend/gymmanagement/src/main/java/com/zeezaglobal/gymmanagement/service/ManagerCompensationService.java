package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.CompensationRequest;
import com.zeezaglobal.gymmanagement.dto.CompensationResponse;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Manager;
import com.zeezaglobal.gymmanagement.entity.ManagerCompensation;
import com.zeezaglobal.gymmanagement.entity.PayType;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.ManagerCompensationRepository;
import com.zeezaglobal.gymmanagement.util.CurrencyCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagerCompensationService {

    private final ManagerCompensationRepository compensationRepository;
    private final GymService gymService;
    private final ManagerService managerService;

    public CompensationResponse get(Long gymId, Long managerId) {
        assertAssignedToGym(gymId, managerId);
        ManagerCompensation compensation = compensationRepository.findByGymIdAndManagerId(gymId, managerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No compensation configured for manager " + managerId + " at gym " + gymId));
        return CompensationResponse.fromEntity(compensation);
    }

    public CompensationResponse upsert(Long gymId, Long managerId, CompensationRequest request) {
        Gym gym = assertAssignedToGym(gymId, managerId);
        Manager manager = managerService.getManagerOrThrow(managerId);
        validate(request);

        ManagerCompensation compensation = compensationRepository.findByGymIdAndManagerId(gymId, managerId)
                .orElseGet(() -> {
                    ManagerCompensation created = new ManagerCompensation();
                    created.setGym(gym);
                    created.setManager(manager);
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

    private Gym assertAssignedToGym(Long gymId, Long managerId) {
        Gym gym = gymService.getGymOrThrow(gymId);
        Manager manager = managerService.getManagerOrThrow(managerId);
        boolean assigned = gym.getManagers().stream().anyMatch(m -> m.getId().equals(manager.getId()))
                || (gym.getOwner() != null && gym.getOwner().getId().equals(manager.getId()));
        if (!assigned) {
            throw new ResourceNotFoundException("Manager " + managerId + " is not assigned to gym " + gymId);
        }
        return gym;
    }
}
