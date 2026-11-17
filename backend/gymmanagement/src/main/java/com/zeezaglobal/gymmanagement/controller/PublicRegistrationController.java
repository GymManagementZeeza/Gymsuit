package com.zeezaglobal.gymmanagement.controller;

import com.zeezaglobal.gymmanagement.dto.MemberRequest;
import com.zeezaglobal.gymmanagement.dto.MemberResponse;
import com.zeezaglobal.gymmanagement.dto.MemberSubscriptionResponse;
import com.zeezaglobal.gymmanagement.dto.PublicGymResponse;
import com.zeezaglobal.gymmanagement.dto.PublicRegisterRequest;
import com.zeezaglobal.gymmanagement.dto.PublicRegisterResponse;
import com.zeezaglobal.gymmanagement.dto.SubscribeRequest;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.security.AuthRateLimiter;
import com.zeezaglobal.gymmanagement.service.GymService;
import com.zeezaglobal.gymmanagement.service.MemberService;
import com.zeezaglobal.gymmanagement.service.MemberSubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Self-service member registration, opened from a gym's QR code — no login.
 * The QR link is the credential, the same as a trainer-invite link. The member
 * record is created with a PENDING plan; the gym confirms the UPI payment from
 * the member list (Take payment), which is what activates the plan — UPI apps
 * don't report back to the website, so there is no automatic confirmation.
 */
@RestController
@RequestMapping("/api/public/gyms/{gymId}")
@RequiredArgsConstructor
public class PublicRegistrationController {

    private final GymService gymService;
    private final MemberService memberService;
    private final MemberSubscriptionService subscriptionService;
    private final AuthRateLimiter rateLimiter;

    @GetMapping
    public PublicGymResponse gym(@PathVariable Long gymId) {
        Gym gym = gymService.getGymOrThrow(gymId);
        return PublicGymResponse.fromEntity(gym);
    }

    @PostMapping("/register")
    public ResponseEntity<PublicRegisterResponse> register(
            @PathVariable Long gymId,
            @Valid @RequestBody PublicRegisterRequest request,
            HttpServletRequest httpRequest) {
        rateLimiter.check(AuthRateLimiter.Action.PUBLIC_REGISTER, request.email(), httpRequest);
        if (!Boolean.TRUE.equals(request.waiverAccepted())
                || request.waiverSignature() == null || request.waiverSignature().isBlank()
                || !request.waiverSignature().startsWith("data:image/png;base64,")) {
            throw new BadRequestException("A signed waiver is required to register.");
        }
        MemberRequest memberRequest = new MemberRequest(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.dateOfBirth(),
                request.gender(),
                request.heightCm(),
                request.weightKg(),
                request.bloodGroup(),
                request.medicalNotes(),
                request.emergencyContactName(),
                request.emergencyContactPhone(),
                request.emergencyContactRelationship(),
                true,
                request.waiverSignature(),
                request.joinDate() != null ? request.joinDate() : LocalDate.now()
        );
        MemberResponse member = memberService.create(gymId, memberRequest);
        MemberSubscriptionResponse subscription = subscriptionService.subscribe(
                gymId, member.id(), new SubscribeRequest(request.planId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new PublicRegisterResponse(
                member.id(), subscription.id(), subscription.status(), subscription.planName()));
    }
}
