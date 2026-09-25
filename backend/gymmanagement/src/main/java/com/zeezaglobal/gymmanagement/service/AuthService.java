package com.zeezaglobal.gymmanagement.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.zeezaglobal.gymmanagement.dto.GoogleLoginRequest;
import com.zeezaglobal.gymmanagement.dto.LoginRequest;
import com.zeezaglobal.gymmanagement.dto.LoginResponse;
import com.zeezaglobal.gymmanagement.entity.User;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.repository.UserRepository;
import com.zeezaglobal.gymmanagement.security.GoogleIdTokenVerifierService;
import com.zeezaglobal.gymmanagement.security.JwtService;
import com.zeezaglobal.gymmanagement.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final GoogleIdTokenVerifierService googleIdTokenVerifierService;
    private final UserRepository userRepository;
    private final OtpService otpService;

    public LoginResponse login(LoginRequest request) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return issueSession(principal);
    }

    public LoginResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = googleIdTokenVerifierService.verify(request.idToken());
        if (payload == null) {
            throw new BadRequestException("Invalid Google sign-in token");
        }
        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new BadRequestException("Google account email is not verified");
        }

        String email = payload.getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException(
                        "No account found for " + email + ". Contact your gym admin to get access."));

        if (!user.isEnabled()) {
            throw new BadRequestException("This account has been disabled");
        }

        return issueSession(new UserPrincipal(user));
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
}
