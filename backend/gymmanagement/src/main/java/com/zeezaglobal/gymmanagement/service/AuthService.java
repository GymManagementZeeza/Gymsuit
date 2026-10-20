package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.dto.LoginRequest;
import com.zeezaglobal.gymmanagement.dto.LoginResponse;
import com.zeezaglobal.gymmanagement.entity.Challenge;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.exception.ResourceNotFoundException;
import com.zeezaglobal.gymmanagement.repository.ChallengeDailyProgressRepository;
import com.zeezaglobal.gymmanagement.repository.ChallengeParticipantRepository;
import com.zeezaglobal.gymmanagement.repository.ChallengeRepository;
import com.zeezaglobal.gymmanagement.repository.DailyHealthRecordRepository;
import com.zeezaglobal.gymmanagement.repository.HealthCanonicalRecordRepository;
import com.zeezaglobal.gymmanagement.repository.HealthDeviceRepository;
import com.zeezaglobal.gymmanagement.repository.HealthRawRecordRepository;
import com.zeezaglobal.gymmanagement.repository.HealthRecordTombstoneRepository;
import com.zeezaglobal.gymmanagement.repository.HealthSourcePreferenceRepository;
import com.zeezaglobal.gymmanagement.repository.HealthUserSyncStateRepository;
import com.zeezaglobal.gymmanagement.repository.OtpCodeRepository;
import com.zeezaglobal.gymmanagement.repository.RefreshTokenRepository;
import com.zeezaglobal.gymmanagement.repository.UserPointTransactionRepository;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.repository.WorkoutRecordRepository;
import com.zeezaglobal.gymmanagement.security.JwtService;
import com.zeezaglobal.gymmanagement.security.SecurityService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final SecurityService securityService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DailyHealthRecordRepository dailyHealthRecordRepository;
    private final WorkoutRecordRepository workoutRecordRepository;
    private final ChallengeParticipantRepository challengeParticipantRepository;
    private final ChallengeDailyProgressRepository challengeDailyProgressRepository;
    private final ChallengeRepository challengeRepository;
    private final UserPointTransactionRepository userPointTransactionRepository;
    private final HealthDeviceRepository healthDeviceRepository;
    private final HealthUserSyncStateRepository healthUserSyncStateRepository;
    private final HealthSourcePreferenceRepository healthSourcePreferenceRepository;
    private final HealthCanonicalRecordRepository healthCanonicalRecordRepository;
    private final HealthRawRecordRepository healthRawRecordRepository;
    private final HealthRecordTombstoneRepository healthRecordTombstoneRepository;
    private final OtpCodeRepository otpCodeRepository;

    public LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return issueSession(principal);
    }

    public boolean emailExists(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    public void requestOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No account found for " + email));
        if (!user.isEnabled()) {
            throw new BadRequestException("This account has been disabled");
        }
        otpService.generate(email);
    }

    public LoginResponse verifyOtp(String email, String code) {
        otpService.verify(email, code);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No account found for " + email));
        if (!user.isEnabled()) {
            throw new BadRequestException("This account has been disabled");
        }

        return issueSession(new UserPrincipal(user));
    }

    public LoginResponse issueSession(UserPrincipal principal) {
        String token = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.createToken(principal);
        return new LoginResponse(token, principal.getRole().name(), principal.getGymId(), principal.getTrainerId(), principal.getMemberId(), principal.getManagerId(), refreshToken);
    }

    public LoginResponse refreshSession(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadRequestException("Invalid refresh token");
        }
        RefreshTokenService.RefreshedSession refreshed = refreshTokenService.rotate(rawRefreshToken);
        UserPrincipal principal = refreshed.principal();
        String token = jwtService.generateToken(principal);
        return new LoginResponse(token, principal.getRole().name(), principal.getGymId(), principal.getTrainerId(), principal.getMemberId(), principal.getManagerId(), refreshed.refreshToken());
    }

    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    /**
     * Self-service account deletion (App Store Guideline 5.1.1(v)).
     * Deletes the caller's account and their personal data: all refresh
     * tokens (so the session cannot be renewed), health records, workouts,
     * challenge participation/progress, points, registered devices, sync
     * state, and pending OTP codes. Challenges the user created are
     * retained for the other participants with the creator cleared.
     * Gym business records (member/trainer/
     * manager profiles, payments, check-ins, subscriptions) are retained —
     * they belong to the gym, not the login account. The client's access
     * token is discarded by the client; it expires on its own.
     */
    @Transactional
    public void deleteOwnAccount() {
        UserPrincipal principal = securityService.currentUser();
        if (principal == null) {
            throw new BadRequestException("Not authenticated");
        }
        User user = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getUserId()));
        Long userId = user.getId();

        refreshTokenRepository.deleteAllByUserId(userId);
        dailyHealthRecordRepository.deleteAllByUserId(userId);
        workoutRecordRepository.deleteAllByUserId(userId);
        challengeParticipantRepository.deleteAllByUserId(userId);
        challengeDailyProgressRepository.deleteAllByUserId(userId);
        userPointTransactionRepository.deleteAllByUserId(userId);
        healthDeviceRepository.deleteAllByUserId(userId);
        healthUserSyncStateRepository.deleteAllByUserId(userId);
        healthSourcePreferenceRepository.deleteAllByUserId(userId);
        healthCanonicalRecordRepository.deleteAllByUserId(userId);
        healthRawRecordRepository.deleteAllByUserId(userId);
        healthRecordTombstoneRepository.deleteAllByUserId(userId);
        otpCodeRepository.deleteAllByEmail(user.getEmail());
        // Challenges created by this user stay for the other participants;
        // clear the creator so the non-nullable FK does not block the delete.
        for (Challenge challenge : challengeRepository.findAllByCreatedBy(user)) {
            challenge.setCreatedBy(null);
        }
        userRepository.delete(user);
    }
}
