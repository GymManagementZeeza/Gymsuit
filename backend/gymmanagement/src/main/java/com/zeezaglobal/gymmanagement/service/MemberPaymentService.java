package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.CashPaymentRequest;
import com.zeezaglobal.gymmanagement.dto.JoiningFeePaymentRequest;
import com.zeezaglobal.gymmanagement.dto.ManualPaymentRequest;
import com.zeezaglobal.gymmanagement.dto.MemberPaymentResponse;
import com.zeezaglobal.gymmanagement.dto.OnlinePaymentRequest;
import com.zeezaglobal.gymmanagement.dto.TakePlanPaymentRequest;
import com.zeezaglobal.gymmanagement.entity.ActivityType;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.MemberPayment;
import com.zeezaglobal.gymmanagement.entity.MemberPaymentStatus;
import com.zeezaglobal.gymmanagement.entity.MemberSubscription;
import com.zeezaglobal.gymmanagement.entity.PaymentMethod;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.payment.PaymentGatewayClient;
import com.zeezaglobal.gymmanagement.payment.PaymentGatewayClientResolver;
import com.zeezaglobal.gymmanagement.payment.PaymentGatewayRequest;
import com.zeezaglobal.gymmanagement.payment.PaymentGatewayResult;
import com.zeezaglobal.gymmanagement.repository.MemberPaymentRepository;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import com.zeezaglobal.gymmanagement.util.CurrencyCodes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberPaymentService {

    private final MemberPaymentRepository memberPaymentRepository;
    private final MemberRepository memberRepository;
    private final GymService gymService;
    private final MemberSubscriptionService memberSubscriptionService;
    private final PaymentGatewayClientResolver gatewayClientResolver;
    private final SecurityService securityService;
    private final GymActivityService activityService;

    public MemberPaymentResponse recordCashPayment(Long gymId, Long memberId, CashPaymentRequest request) {
        return recordManualPayment(gymId, memberId, request.subscriptionId(), request.amount(), request.currency(),
                PaymentMethod.CASH, request.notes());
    }

    /** Records a payment staff confirmed was received outside any gateway — cash in hand, or a scanned UPI QR. */
    public MemberPaymentResponse recordManualPayment(Long gymId, Long memberId, ManualPaymentRequest request) {
        if (request.paymentMethod() != PaymentMethod.CASH && request.paymentMethod() != PaymentMethod.UPI) {
            throw new BadRequestException(
                    "Only CASH or UPI payments can be recorded manually; use the online payment endpoint for gateway-processed methods");
        }
        return recordManualPayment(gymId, memberId, request.subscriptionId(), request.amount(), request.currency(),
                request.paymentMethod(), request.notes());
    }

    private MemberPaymentResponse recordManualPayment(
            Long gymId, Long memberId, Long subscriptionId, BigDecimal amount, String currency,
            PaymentMethod method, String notes) {
        Gym gym = gymService.getGymOrThrow(gymId);
        Member member = getMemberOrThrow(gymId, memberId);
        MemberSubscription subscription = resolveSubscription(gymId, subscriptionId);

        MemberPayment payment = new MemberPayment();
        payment.setGym(gym);
        payment.setMember(member);
        payment.setSubscription(subscription);
        payment.setAmount(amount);
        payment.setCurrency(CurrencyCodes.validate(currency));
        payment.setPaymentMethod(method);
        payment.setStatus(MemberPaymentStatus.SUCCEEDED);
        payment.setNotes(notes);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setPaidAt(LocalDateTime.now());

        if (subscription != null) {
            payment.setPeriodStart(subscription.getCurrentPeriodStart());
            payment.setPeriodEnd(subscription.getCurrentPeriodEnd());
            memberSubscriptionService.advancePeriod(subscription);
        }

        payment = memberPaymentRepository.save(payment);
        activityService.record(gym, ActivityType.PAYMENT_RECEIVED,
                "Payment received " + payment.getCurrency() + " " + payment.getAmount()
                        + " from " + member.getFirstName() + " " + member.getLastName()
                        + " (" + method.name() + ")");
        return MemberPaymentResponse.fromEntity(payment);
    }

    /**
     * Takes payment for a member's pending plan and activates it in the same transaction — a plan only
     * becomes the member's active plan once this succeeds, never just from being assigned.
     */
    @Transactional
    public MemberPaymentResponse recordPlanPayment(Long gymId, Long memberId, TakePlanPaymentRequest request) {
        if (request.paymentMethod() == PaymentMethod.STRIPE || request.paymentMethod() == PaymentMethod.RAZORPAY) {
            throw new BadRequestException(
                    "Gateway-processed methods aren't supported for taking a plan payment here; use CASH, UPI, BANK_TRANSFER, or OTHER");
        }

        Gym gym = gymService.getGymOrThrow(gymId);
        Member member = getMemberOrThrow(gymId, memberId);
        MemberSubscription subscription = memberSubscriptionService.activatePendingSubscription(gymId, memberId);

        MemberPayment payment = new MemberPayment();
        payment.setGym(gym);
        payment.setMember(member);
        payment.setSubscription(subscription);
        payment.setAmount(subscription.getPlan().getPrice());
        payment.setCurrency(subscription.getPlan().getCurrency());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setStatus(MemberPaymentStatus.SUCCEEDED);
        payment.setNotes("Payment for " + subscription.getPlan().getName() + " plan");
        payment.setPeriodStart(subscription.getCurrentPeriodStart());
        payment.setPeriodEnd(subscription.getCurrentPeriodEnd());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setPaidAt(LocalDateTime.now());

        payment = memberPaymentRepository.save(payment);
        activityService.record(gym, ActivityType.PAYMENT_RECEIVED,
                "Payment received " + payment.getCurrency() + " " + payment.getAmount()
                        + " from " + member.getFirstName() + " " + member.getLastName()
                        + " for the " + subscription.getPlan().getName() + " plan (" + request.paymentMethod().name() + ")");
        return MemberPaymentResponse.fromEntity(payment);
    }

    /**
     * Charges a new member's one-time joining fee at the amount the gym configured — never a client-supplied
     * amount — and marks them as having finished registration. Can only happen once per member.
     */
    @Transactional
    public MemberPaymentResponse payJoiningFee(Long gymId, Long memberId, JoiningFeePaymentRequest request) {
        if (request.paymentMethod() == PaymentMethod.STRIPE || request.paymentMethod() == PaymentMethod.RAZORPAY) {
            throw new BadRequestException(
                    "Gateway-processed methods aren't supported for the joining fee here; use CASH, UPI, BANK_TRANSFER, or OTHER");
        }

        Gym gym = gymService.getGymOrThrow(gymId);
        Member member = getMemberOrThrow(gymId, memberId);

        if (member.isJoiningFeePaid()) {
            throw new BadRequestException("This member has already paid their joining fee");
        }
        if (gym.getJoiningFee() == null) {
            throw new BadRequestException("This gym hasn't set a joining fee yet — set one in Settings first");
        }

        MemberPayment payment = new MemberPayment();
        payment.setGym(gym);
        payment.setMember(member);
        payment.setAmount(gym.getJoiningFee());
        payment.setCurrency(gym.getJoiningFeeCurrency() != null ? gym.getJoiningFeeCurrency() : "INR");
        payment.setPaymentMethod(request.paymentMethod());
        payment.setStatus(MemberPaymentStatus.SUCCEEDED);
        payment.setNotes(request.notes() != null ? request.notes() : "Joining fee");
        payment.setCreatedAt(LocalDateTime.now());
        payment.setPaidAt(LocalDateTime.now());
        payment = memberPaymentRepository.save(payment);

        member.setJoiningFeePaid(true);
        member.setJoiningFeePaidAt(LocalDateTime.now());
        memberRepository.save(member);

        activityService.record(gym, ActivityType.PAYMENT_RECEIVED,
                "Joining fee received " + payment.getCurrency() + " " + payment.getAmount()
                        + " from " + member.getFirstName() + " " + member.getLastName()
                        + " (" + request.paymentMethod().name() + ")");
        return MemberPaymentResponse.fromEntity(payment);
    }

    public MemberPaymentResponse initiateOnlinePayment(Long gymId, Long memberId, OnlinePaymentRequest request) {
        if (request.paymentMethod() == PaymentMethod.CASH) {
            throw new BadRequestException("Use the cash payment endpoint for CASH payments");
        }

        Gym gym = gymService.getGymOrThrow(gymId);
        Member member = getMemberOrThrow(gymId, memberId);
        MemberSubscription subscription = resolveSubscription(gymId, request.subscriptionId());

        MemberPayment payment = new MemberPayment();
        payment.setGym(gym);
        payment.setMember(member);
        payment.setSubscription(subscription);
        payment.setAmount(request.amount());
        payment.setCurrency(CurrencyCodes.validate(request.currency()));
        payment.setPaymentMethod(request.paymentMethod());
        payment.setStatus(MemberPaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());
        if (subscription != null) {
            payment.setPeriodStart(subscription.getCurrentPeriodStart());
            payment.setPeriodEnd(subscription.getCurrentPeriodEnd());
        }
        payment = memberPaymentRepository.save(payment);

        PaymentGatewayClient client = gatewayClientResolver.resolve(request.paymentMethod());
        PaymentGatewayResult result = client.initiate(new PaymentGatewayRequest(
                gymId, memberId, payment.getId(), payment.getAmount(), payment.getCurrency(),
                "Membership payment for member " + memberId));

        payment.setGatewayReference(result.referenceId());
        payment.setStatus(result.status());

        return MemberPaymentResponse.fromEntity(memberPaymentRepository.save(payment));
    }

    public List<MemberPaymentResponse> findVisible(Long gymId) {
        gymService.getGymOrThrow(gymId);
        UserPrincipal user = securityService.currentUser();
        if (user == null) {
            throw new BadRequestException("Not authenticated");
        }

        List<MemberPayment> payments = switch (user.getRole()) {
            case ADMIN, OWNER, MANAGER -> memberPaymentRepository.findAllByGymIdOrderByCreatedAtDesc(gymId);
            case MEMBER -> memberPaymentRepository.findAllByGymIdAndMemberIdOrderByCreatedAtDesc(gymId, user.getMemberId());
            case TRAINER -> throw new BadRequestException("Trainers do not have access to member payment records");
        };

        return payments.stream().map(MemberPaymentResponse::fromEntity).toList();
    }

    public List<MemberPaymentResponse> history(Long gymId, Long memberId) {
        gymService.getGymOrThrow(gymId);
        getMemberOrThrow(gymId, memberId);
        return memberPaymentRepository.findAllByGymIdAndMemberIdOrderByCreatedAtDesc(gymId, memberId).stream()
                .map(MemberPaymentResponse::fromEntity)
                .toList();
    }

    public MemberPaymentResponse findById(Long gymId, Long id) {
        return MemberPaymentResponse.fromEntity(getPaymentOrThrow(gymId, id));
    }

    private MemberSubscription resolveSubscription(Long gymId, Long subscriptionId) {
        return subscriptionId != null ? memberSubscriptionService.getByIdOrThrow(gymId, subscriptionId) : null;
    }

    private Member getMemberOrThrow(Long gymId, Long memberId) {
        return memberRepository.findByIdAndGymId(memberId, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found with id: " + memberId + " in gym: " + gymId));
    }

    private MemberPayment getPaymentOrThrow(Long gymId, Long id) {
        return memberPaymentRepository.findByIdAndGymId(id, gymId)
                .orElseThrow(() -> new ResourceNotFoundException("Member payment not found with id: " + id + " in gym: " + gymId));
    }
}
