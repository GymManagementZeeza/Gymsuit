package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.MemberSubscriptionResponse;
import com.zeezaglobal.gymmanagement.dto.SubscribeRequest;
import com.zeezaglobal.gymmanagement.entity.ActivityType;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.MemberSubscription;
import com.zeezaglobal.gymmanagement.entity.MembershipPlan;
import com.zeezaglobal.gymmanagement.entity.SubscriptionStatus;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.MemberSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberSubscriptionService {

    private final MemberSubscriptionRepository subscriptionRepository;
    private final MemberRepository memberRepository;
    private final GymService gymService;
    private final MembershipPlanService membershipPlanService;
    private final GymActivityService activityService;

    public List<MemberSubscriptionResponse> history(Long gymId, Long memberId) {
        gymService.getGymOrThrow(gymId);
        assertMemberInGym(gymId, memberId);
        return subscriptionRepository.findAllByGymIdAndMemberIdOrderByStartDateDesc(gymId, memberId).stream()
                .map(MemberSubscriptionResponse::fromEntity)
                .toList();
    }

    /** Every member's current active subscription in the gym — used by the member directory to show plan + next payment date. */
    public List<MemberSubscriptionResponse> currentForGym(Long gymId) {
        gymService.getGymOrThrow(gymId);
        return subscriptionRepository.findAllByGymIdAndStatus(gymId, SubscriptionStatus.ACTIVE).stream()
                .map(MemberSubscriptionResponse::fromEntity)
                .toList();
    }

    /** Every member with a plan assigned but not yet paid for — used to show the "Take payment" prompt. */
    public List<MemberSubscriptionResponse> pendingForGym(Long gymId) {
        gymService.getGymOrThrow(gymId);
        return subscriptionRepository.findAllByGymIdAndStatus(gymId, SubscriptionStatus.PENDING).stream()
                .map(MemberSubscriptionResponse::fromEntity)
                .toList();
    }

    public MemberSubscriptionResponse current(Long gymId, Long memberId) {
        return MemberSubscriptionResponse.fromEntity(getActiveOrThrow(gymId, memberId));
    }

    /** Assigns a plan to a member. It stays PENDING — not their active plan — until a payment is taken for it. */
    public MemberSubscriptionResponse subscribe(Long gymId, Long memberId, SubscribeRequest request) {
        if (subscriptionRepository.findByGymIdAndMemberIdAndStatus(gymId, memberId, SubscriptionStatus.ACTIVE).isPresent()) {
            throw new ConflictException("Member " + memberId + " already has an active subscription — cancel it first");
        }
        if (subscriptionRepository.findByGymIdAndMemberIdAndStatus(gymId, memberId, SubscriptionStatus.PENDING).isPresent()) {
            throw new ConflictException("Member " + memberId + " already has a plan awaiting payment");
        }
        MembershipPlan plan = requireActivePlan(gymId, request.planId());
        MemberSubscription subscription = assignPendingSubscription(gymId, memberId, plan);
        activityService.record(subscription.getGym(), ActivityType.PLAN_ASSIGNED,
                memberName(subscription.getMember()) + " was assigned the " + plan.getName() + " plan — payment pending");
        return MemberSubscriptionResponse.fromEntity(subscription);
    }

    public MemberSubscriptionResponse cancel(Long gymId, Long memberId) {
        MemberSubscription subscription = getActiveOrThrow(gymId, memberId);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setAutoRenew(false);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription = subscriptionRepository.save(subscription);
        activityService.record(subscription.getGym(), ActivityType.SUBSCRIPTION_CANCELLED,
                memberName(subscription.getMember()) + " cancelled the " + subscription.getPlan().getName() + " plan");
        return MemberSubscriptionResponse.fromEntity(subscription);
    }

    /**
     * Switches a member to a different plan: cancels whatever active/pending subscription they have (if any)
     * and assigns the new one — which, like a fresh assignment, stays PENDING until it's paid for.
     */
    @Transactional
    public MemberSubscriptionResponse changePlan(Long gymId, Long memberId, SubscribeRequest request) {
        MembershipPlan plan = requireActivePlan(gymId, request.planId());

        cancelIfPresent(subscriptionRepository.findByGymIdAndMemberIdAndStatus(gymId, memberId, SubscriptionStatus.ACTIVE));
        cancelIfPresent(subscriptionRepository.findByGymIdAndMemberIdAndStatus(gymId, memberId, SubscriptionStatus.PENDING));

        MemberSubscription subscription = assignPendingSubscription(gymId, memberId, plan);
        activityService.record(subscription.getGym(), ActivityType.PLAN_CHANGED,
                memberName(subscription.getMember()) + " was moved to the " + plan.getName() + " plan — payment pending");
        return MemberSubscriptionResponse.fromEntity(subscription);
    }

    /** Flips a member's pending subscription to active — called once a payment for it has been recorded. */
    MemberSubscription activatePendingSubscription(Long gymId, Long memberId) {
        MemberSubscription subscription = getPendingOrThrow(gymId, memberId);

        LocalDate today = LocalDate.now();
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(today);
        subscription.setCurrentPeriodStart(today);
        subscription.setCurrentPeriodEnd(subscription.getPlan().getBillingCycle().periodEnd(today));
        subscription = subscriptionRepository.save(subscription);

        activityService.record(subscription.getGym(), ActivityType.PLAN_ASSIGNED,
                memberName(subscription.getMember()) + "'s " + subscription.getPlan().getName() + " plan is now active");
        return subscription;
    }

    private void cancelIfPresent(Optional<MemberSubscription> maybeSubscription) {
        maybeSubscription.ifPresent(current -> {
            current.setStatus(SubscriptionStatus.CANCELLED);
            current.setAutoRenew(false);
            current.setCancelledAt(LocalDateTime.now());
            subscriptionRepository.save(current);
        });
    }

    private String memberName(Member member) {
        return member.getFirstName() + " " + member.getLastName();
    }

    private MembershipPlan requireActivePlan(Long gymId, Long planId) {
        MembershipPlan plan = membershipPlanService.getPlanOrThrow(gymId, planId);
        if (!plan.isActive()) {
            throw new BadRequestException("Membership plan " + plan.getId() + " is not active");
        }
        return plan;
    }

    private MemberSubscription assignPendingSubscription(Long gymId, Long memberId, MembershipPlan plan) {
        Gym gym = gymService.getGymOrThrow(gymId);
        Member member = assertMemberInGym(gymId, memberId);

        MemberSubscription subscription = new MemberSubscription();
        subscription.setGym(gym);
        subscription.setMember(member);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setAutoRenew(true);

        return subscriptionRepository.save(subscription);
    }

    /** Advances the subscription to its next billing period — called after a successful renewal payment. */
    void advancePeriod(MemberSubscription subscription) {
        LocalDate newStart = subscription.getCurrentPeriodEnd();
        subscription.setCurrentPeriodStart(newStart);
        subscription.setCurrentPeriodEnd(subscription.getPlan().getBillingCycle().periodEnd(newStart));
        subscriptionRepository.save(subscription);
    }

    MemberSubscription getActiveOrThrow(Long gymId, Long memberId) {
        gymService.getGymOrThrow(gymId);
        assertMemberInGym(gymId, memberId);
        return subscriptionRepository.findByGymIdAndMemberIdAndStatus(gymId, memberId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Member " + memberId + " has no active subscription"));
    }

    MemberSubscription getPendingOrThrow(Long gymId, Long memberId) {
        gymService.getGymOrThrow(gymId);
        assertMemberInGym(gymId, memberId);
        return subscriptionRepository.findByGymIdAndMemberIdAndStatus(gymId, memberId, SubscriptionStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Member " + memberId + " has no plan awaiting payment"));
    }

    MemberSubscription getByIdOrThrow(Long gymId, Long subscriptionId) {
        return subscriptionRepository.findByIdAndGymId(subscriptionId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + subscriptionId + " in gym: " + gymId));
    }

    private Member assertMemberInGym(Long gymId, Long memberId) {
        return memberRepository.findByIdAndGymId(memberId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId + " in gym: " + gymId));
    }
}
