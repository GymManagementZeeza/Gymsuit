package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.MobileAuthResponse;
import com.zeezaglobal.gymmanagement.dto.MobileForgotPasswordRequest;
import com.zeezaglobal.gymmanagement.dto.MobileLoginRequest;
import com.zeezaglobal.gymmanagement.dto.MobileRegisterRequest;
import com.zeezaglobal.gymmanagement.dto.MobileRegisterWithOtpRequest;
import com.zeezaglobal.gymmanagement.dto.MobileResetPasswordRequest;
import com.zeezaglobal.gymmanagement.dto.MobileSendOtpRequest;
import com.zeezaglobal.gymmanagement.dto.MobileVerifyOtpLoginRequest;
import com.zeezaglobal.gymmanagement.dto.RefreshRequest;
import com.zeezaglobal.gymmanagement.entity.Gym;
import com.zeezaglobal.gymmanagement.entity.Member;
import com.zeezaglobal.gymmanagement.entity.Role;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ConflictException;
import com.zeezaglobal.gymmanagement.repository.GymRepository;
import com.zeezaglobal.gymmanagement.repository.MemberRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.JwtService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MobileAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final GymRepository gymRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailService emailService;

    public void sendOtp(MobileSendOtpRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();
        String mode = request.mode() != null ? request.mode().trim().toLowerCase() : "login";

        if ("register".equals(mode)) {
            if (userRepository.findByEmail(cleanEmail).isPresent()) {
                throw new ConflictException("An account with email " + cleanEmail + " already exists");
            }
        } else {
            // mode = "login"
            User user = userRepository.findByEmail(cleanEmail)
                    .orElseThrow(() -> new BadRequestException("No account found for " + cleanEmail + ". Please register first."));
            if (!user.isEnabled()) {
                throw new BadRequestException("This account is disabled. Please contact your gym administrator.");
            }
        }

        otpService.generate(cleanEmail);
    }

    public MobileAuthResponse verifyOtpLogin(MobileVerifyOtpLoginRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();
        otpService.verify(cleanEmail, request.otp().trim());

        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new BadRequestException("No account found for " + cleanEmail));

        if (!user.isEnabled()) {
            throw new BadRequestException("This account is disabled. Please contact your gym administrator.");
        }

        UserPrincipal principal = new UserPrincipal(user);
        return issueMobileSession(principal, user);
    }

    @Transactional
    public MobileAuthResponse registerWithOtp(MobileRegisterWithOtpRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();

        // 1. Verify OTP first
        otpService.verify(cleanEmail, request.otp().trim());

        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new ConflictException("An account with email " + cleanEmail + " already exists");
        }

        // Determine Gym association
        Gym gym = null;
        if (request.gymId() != null) {
            gym = gymRepository.findById(request.gymId()).orElse(null);
        }
        if (gym == null) {
            gym = gymRepository.findAll().stream().findFirst().orElseGet(() -> {
                Gym defaultGym = new Gym();
                defaultGym.setName("GymSuit Global");
                defaultGym.setEmail("support@gymsuit.app");
                defaultGym.setPhone(request.phone());
                return gymRepository.save(defaultGym);
            });
        }

        // Create Member entity
        Member member = new Member();
        member.setGym(gym);
        member.setFirstName(request.firstName().trim());
        member.setLastName(request.lastName().trim());
        member.setEmail(cleanEmail);
        member.setPhone(request.phone().trim());
        member.setJoinDate(java.time.LocalDate.now());
        member.setWaiverAccepted(true);
        member.setJoiningFeePaid(true);
        member = memberRepository.save(member);

        // Create User credentials (passwordless: random UUID encrypted token)
        User user = new User();
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setRole(Role.MEMBER);
        user.setEnabled(true);
        user.setGym(gym);
        user.setMember(member);
        user = userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        return issueMobileSession(principal, user);
    }

    public MobileAuthResponse login(MobileLoginRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(cleanEmail, request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!user.isEnabled()) {
            throw new BadRequestException("This account is disabled. Please contact your gym administrator.");
        }

        return issueMobileSession(principal, user);
    }

    @Transactional
    public MobileAuthResponse register(MobileRegisterRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();

        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new ConflictException("An account with email " + cleanEmail + " already exists");
        }

        // Determine Gym association (provided ID, or first available gym, or create default system gym)
        Gym gym = null;
        if (request.gymId() != null) {
            gym = gymRepository.findById(request.gymId()).orElse(null);
        }
        if (gym == null) {
            gym = gymRepository.findAll().stream().findFirst().orElseGet(() -> {
                Gym defaultGym = new Gym();
                defaultGym.setName("GymSuit Global");
                defaultGym.setEmail("support@gymsuit.app");
                defaultGym.setPhone(request.phone());
                return gymRepository.save(defaultGym);
            });
        }

        // Create Member entity
        Member member = new Member();
        member.setGym(gym);
        member.setFirstName(request.firstName().trim());
        member.setLastName(request.lastName().trim());
        member.setEmail(cleanEmail);
        member.setPhone(request.phone().trim());
        member.setJoinDate(java.time.LocalDate.now());
        member.setWaiverAccepted(true);
        member.setJoiningFeePaid(true);
        member = memberRepository.save(member);

        // Create User credentials
        User user = new User();
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.MEMBER);
        user.setEnabled(true);
        user.setGym(gym);
        user.setMember(member);
        user = userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        return issueMobileSession(principal, user);
    }

    public void forgotPassword(MobileForgotPasswordRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();
        userRepository.findByEmail(cleanEmail).ifPresent(user -> {
            if (user.isEnabled()) {
                otpService.generate(cleanEmail);
            }
        });
        // Note: For security, do not leak whether the email exists or not
    }

    @Transactional
    public void resetPassword(MobileResetPasswordRequest request) {
        String cleanEmail = request.email().trim().toLowerCase();
        otpService.verify(cleanEmail, request.otp().trim());

        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new BadRequestException("No account found for " + cleanEmail));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Password successfully reset for mobile user {}", cleanEmail);
    }

    public MobileAuthResponse refreshToken(RefreshRequest request) {
        if (request == null || request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new BadRequestException("Missing refresh token");
        }

        RefreshTokenService.RefreshedSession refreshed = refreshTokenService.rotate(request.refreshToken());
        UserPrincipal principal = refreshed.principal();

        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found"));

        String token = jwtService.generateToken(principal);

        Member member = user.getMember();
        String firstName = member != null ? member.getFirstName() : user.getEmail();
        String lastName = member != null ? member.getLastName() : "";
        Long memberId = member != null ? member.getId() : null;
        Long gymId = user.getGym() != null ? user.getGym().getId() : null;
        String gymName = user.getGym() != null ? user.getGym().getName() : null;

        return new MobileAuthResponse(
                token,
                refreshed.refreshToken(),
                principal.getRole().name(),
                user.getEmail(),
                firstName,
                lastName,
                memberId,
                gymId,
                gymName
        );
    }

    private MobileAuthResponse issueMobileSession(UserPrincipal principal, User user) {
        String token = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.createToken(principal);

        Member member = user.getMember();
        String firstName = member != null ? member.getFirstName() : user.getEmail();
        String lastName = member != null ? member.getLastName() : "";
        Long memberId = member != null ? member.getId() : null;
        Long gymId = user.getGym() != null ? user.getGym().getId() : null;
        String gymName = user.getGym() != null ? user.getGym().getName() : null;

        return new MobileAuthResponse(
                token,
                refreshToken,
                principal.getRole().name(),
                user.getEmail(),
                firstName,
                lastName,
                memberId,
                gymId,
                gymName
        );
    }
}
