package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.TransactionRequest;
import com.zeezaglobal.gymmanagement.dto.TransactionResponse;
import com.zeezaglobal.gymmanagement.entity.ActivityType;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.GymTransaction;
import com.zeezaglobal.gymmanagement.entity.Manager;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.Trainer;
import com.zeezaglobal.gymmanagement.entity.TransactionDirection;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.GymTransactionRepository;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.TrainerRepository;
import com.zeezaglobal.gymmanagement.util.CurrencyCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GymTransactionService {

    private final GymTransactionRepository transactionRepository;
    private final MemberRepository memberRepository;
    private final TrainerRepository trainerRepository;
    private final GymService gymService;
    private final ManagerService managerService;
    private final GymActivityService activityService;

    public List<TransactionResponse> listForGym(Long gymId) {
        gymService.getGymOrThrow(gymId);
        return transactionRepository.findAllByGymIdOrderByOccurredOnDescCreatedAtDesc(gymId).stream()
                .map(TransactionResponse::fromEntity)
                .toList();
    }

    public TransactionResponse create(Long gymId, TransactionRequest request) {
        Gym gym = gymService.getGymOrThrow(gymId);
        Member member = request.memberId() != null
                ? memberRepository.findByIdAndGymId(request.memberId(), gymId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Member not found with id: " + request.memberId() + " in gym: " + gymId))
                : null;
        Trainer trainer = request.trainerId() != null
                ? trainerRepository.findByIdAndGymId(request.trainerId(), gymId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Trainer not found with id: " + request.trainerId() + " in gym: " + gymId))
                : null;
        Manager manager = request.managerId() != null ? getAssignedManagerOrThrow(gym, request.managerId()) : null;

        GymTransaction transaction = new GymTransaction();
        transaction.setGym(gym);
        transaction.setMember(member);
        transaction.setTrainer(trainer);
        transaction.setManager(manager);
        transaction.setDirection(request.direction());
        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());
        transaction.setCurrency(CurrencyCodes.validate(request.currency()));
        transaction.setPaymentMethod(request.paymentMethod());
        transaction.setOccurredOn(request.occurredOn());
        transaction.setNotes(request.notes());
        transaction.setCreatedAt(LocalDateTime.now());

        transaction = transactionRepository.save(transaction);

        boolean isIncome = request.direction() == TransactionDirection.INCOME;
        String verb = isIncome ? "Recorded income" : "Recorded expense";
        String who = member != null
                ? " from " + member.getFirstName() + " " + member.getLastName()
                : trainer != null
                        ? " to " + trainer.getFirstName() + " " + trainer.getLastName()
                        : manager != null ? " to " + manager.getFirstName() + " " + manager.getLastName() : "";
        activityService.record(gym, isIncome ? ActivityType.INCOME_RECORDED : ActivityType.EXPENSE_RECORDED,
                verb + " of " + transaction.getCurrency() + " " + transaction.getAmount()
                        + " — " + transaction.getDescription() + who);

        return TransactionResponse.fromEntity(transaction);
    }

    private Manager getAssignedManagerOrThrow(Gym gym, Long managerId) {
        Manager manager = managerService.getManagerOrThrow(managerId);
        boolean assigned = gym.getManagers().stream().anyMatch(m -> m.getId().equals(manager.getId()))
                || (gym.getOwner() != null && gym.getOwner().getId().equals(manager.getId()));
        if (!assigned) {
            throw new ResourceNotFoundException("Manager " + managerId + " is not assigned to gym " + gym.getId());
        }
        return manager;
    }
}
