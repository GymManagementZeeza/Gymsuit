package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.PaymentRequest;
import com.zeezaglobal.gymmanagement.dto.PaymentResponse;
import com.zeezaglobal.gymmanagement.dto.PaymentStatusUpdateRequest;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Manager;
import com.zeezaglobal.gymmanagement.entity.PayeeType;
import com.zeezaglobal.gymmanagement.entity.Payment;
import com.zeezaglobal.gymmanagement.entity.PaymentStatus;
import com.zeezaglobal.gymmanagement.entity.Trainer;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.PaymentRepository;
import com.zeezaglobal.gymmanagement.repository.TrainerRepository;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.util.CurrencyCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TrainerRepository trainerRepository;
    private final GymService gymService;
    private final ManagerService managerService;
    private final SecurityService securityService;

    public PaymentResponse create(Long gymId, PaymentRequest request) {
        Gym gym = gymService.getGymOrThrow(gymId);

        Trainer trainer = null;
        Manager manager = null;

        if (request.payeeType() == PayeeType.TRAINER) {
            if (request.trainerId() == null || request.managerId() != null) {
                throw new BadRequestException("A TRAINER payment must set trainerId only");
            }
            trainer = trainerRepository.findByIdAndGymId(request.trainerId(), gymId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Trainer not found with id: " + request.trainerId() + " in gym: " + gymId));
        } else {
            if (request.managerId() == null || request.trainerId() != null) {
                throw new BadRequestException("A MANAGER payment must set managerId only");
            }
            manager = managerService.getManagerOrThrow(request.managerId());
            Long managerId = manager.getId();
            boolean assigned = gym.getManagers().stream().anyMatch(m -> m.getId().equals(managerId))
                    || (gym.getOwner() != null && gym.getOwner().getId().equals(managerId));
            if (!assigned) {
                throw new ResourceNotFoundException("Manager " + request.managerId() + " is not assigned to gym " + gymId);
            }
        }

        if (request.periodEnd() != null && request.periodEnd().isBefore(request.periodStart())) {
            throw new BadRequestException("periodEnd cannot be before periodStart");
        }

        Payment payment = new Payment();
        payment.setGym(gym);
        payment.setPayeeType(request.payeeType());
        payment.setTrainer(trainer);
        payment.setManager(manager);
        payment.setAmount(request.amount());
        payment.setCurrency(CurrencyCodes.validate(request.currency()));
        payment.setPeriodStart(request.periodStart());
        payment.setPeriodEnd(request.periodEnd());
        payment.setHoursWorked(request.hoursWorked());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setNotes(request.notes());
        payment.setCreatedAt(LocalDateTime.now());

        return PaymentResponse.fromEntity(paymentRepository.save(payment));
    }

    public List<PaymentResponse> findVisible(Long gymId) {
        gymService.getGymOrThrow(gymId);
        UserPrincipal user = securityService.currentUser();
        if (user == null) {
            throw new BadRequestException("Not authenticated");
        }

        List<Payment> payments = switch (user.getRole()) {
            case ADMIN, OWNER -> paymentRepository.findAllByGymIdOrderByPeriodStartDesc(gymId);
            case TRAINER -> paymentRepository.findAllByGymIdAndTrainerIdOrderByPeriodStartDesc(gymId, user.getTrainerId());
            case MANAGER -> paymentRepository.findAllByGymIdAndManagerIdOrderByPeriodStartDesc(gymId, user.getManagerId());
            case MEMBER -> throw new BadRequestException("Members do not have payment records");
        };

        return payments.stream().map(PaymentResponse::fromEntity).toList();
    }

    public PaymentResponse findById(Long gymId, Long id) {
        return PaymentResponse.fromEntity(getPaymentOrThrow(gymId, id));
    }

    public PaymentResponse updateStatus(Long gymId, Long id, PaymentStatusUpdateRequest request) {
        Payment payment = getPaymentOrThrow(gymId, id);
        payment.setStatus(request.status());
        if (request.status() == PaymentStatus.PAID) {
            payment.setPaidAt(LocalDateTime.now());
        }
        return PaymentResponse.fromEntity(paymentRepository.save(payment));
    }

    public void delete(Long gymId, Long id) {
        Payment payment = getPaymentOrThrow(gymId, id);
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new ConflictException("Payment " + id + " has already been paid and cannot be deleted");
        }
        paymentRepository.delete(payment);
    }

    private Payment getPaymentOrThrow(Long gymId, Long id) {
        return paymentRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id + " in gym: " + gymId));
    }
}
