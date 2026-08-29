package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.MembershipPlanRequest;
import com.zeezaglobal.gymmanagement.dto.MembershipPlanResponse;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.MembershipPlan;
import com.zeezaglobal.gymmanagement.entity.SubscriptionStatus;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.MemberSubscriptionRepository;
import com.zeezaglobal.gymmanagement.repository.MembershipPlanRepository;
import com.zeezaglobal.gymmanagement.util.CurrencyCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPlanService {

    private final MembershipPlanRepository membershipPlanRepository;
    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final GymService gymService;

    public List<MembershipPlanResponse> findAllByGym(Long gymId) {
        gymService.getGymOrThrow(gymId);
        return membershipPlanRepository.findAllByGymId(gymId).stream()
                .map(MembershipPlanResponse::fromEntity)
                .toList();
    }

    /** Plans a prospective member can pick from during self-registration — active plans only. */
    public List<MembershipPlanResponse> findActiveByGym(Long gymId) {
        gymService.getGymOrThrow(gymId);
        return membershipPlanRepository.findAllByGymId(gymId).stream()
                .filter(MembershipPlan::isActive)
                .map(MembershipPlanResponse::fromEntity)
                .toList();
    }

    public MembershipPlanResponse findById(Long gymId, Long id) {
        return MembershipPlanResponse.fromEntity(getPlanOrThrow(gymId, id));
    }

    public MembershipPlanResponse create(Long gymId, MembershipPlanRequest request) {
        Gym gym = gymService.getGymOrThrow(gymId);
        MembershipPlan plan = new MembershipPlan();
        plan.setGym(gym);
        applyRequest(plan, request);
        return MembershipPlanResponse.fromEntity(membershipPlanRepository.save(plan));
    }

    public MembershipPlanResponse update(Long gymId, Long id, MembershipPlanRequest request) {
        MembershipPlan plan = getPlanOrThrow(gymId, id);
        applyRequest(plan, request);
        return MembershipPlanResponse.fromEntity(membershipPlanRepository.save(plan));
    }

    public void delete(Long gymId, Long id) {
        MembershipPlan plan = getPlanOrThrow(gymId, id);
        if (membershipPlanRepository.countByGymId(gymId) <= 1) {
            throw new BadRequestException("A gym must have at least one membership plan; add another before removing this one");
        }
        if (memberSubscriptionRepository.existsByPlanIdAndStatus(id, SubscriptionStatus.ACTIVE)) {
            throw new BadRequestException("This plan has active members on it — move them to another plan before removing it");
        }
        if (memberSubscriptionRepository.existsByPlanId(id)) {
            throw new BadRequestException(
                    "This plan has subscription history and can't be deleted. Mark it inactive instead to stop offering it to new members.");
        }
        membershipPlanRepository.delete(plan);
    }

    MembershipPlan getPlanOrThrow(Long gymId, Long id) {
        return membershipPlanRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership plan not found with id: " + id + " in gym: " + gymId));
    }

    private void applyRequest(MembershipPlan plan, MembershipPlanRequest request) {
        plan.setName(request.name());
        plan.setDescription(request.description());
        plan.setPrice(request.price());
        plan.setCurrency(CurrencyCodes.validate(request.currency()));
        plan.setBillingCycle(request.billingCycle());
        plan.setActive(request.active() == null || request.active());
    }
}
