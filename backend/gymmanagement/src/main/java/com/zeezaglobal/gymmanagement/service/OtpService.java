package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.entity.OtpCode;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.messaging.EventPublisher;
import com.zeezaglobal.gymmanagement.repository.OtpCodeRepository;
import com.zeezaglobal.gymmanagement.config.TestLoginSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final long OTP_TTL_MINUTES = 5;

    private final OtpCodeRepository otpCodeRepository;
    private final EventPublisher eventPublisher;
    private final TestLoginSupport testLoginSupport;
    private final SecureRandom random = new SecureRandom();

    public void generate(String email) {
        // TEST ONLY: the designated test account always gets the same fixed code and no real email is sent.
        boolean testAccount = testLoginSupport.matches(email);
        String code = testAccount ? testLoginSupport.fixedOtp() : String.format("%06d", random.nextInt(1_000_000));

        OtpCode otp = new OtpCode();
        otp.setEmail(email);
        otp.setCode(code);
        otp.setExpiresAt(Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES));
        otpCodeRepository.save(otp);

        if (testAccount) {
            log.info("Fixed test OTP issued for {} (no email sent)", email);
            return;
        }

        log.info("OTP generated for {} (expires in {} minutes)", email, OTP_TTL_MINUTES);
        // Queued: the request returns immediately and a consumer sends the email via Resend.
        // Failing fast here beats the old behavior of silently returning 200 with no email sent.
        try {
            eventPublisher.publishEmail(email, "Your GymSuit Login Code", EmailService.otpEmailHtml(code));
        } catch (AmqpException e) {
            log.error("Could not queue OTP email for {}: {}", email, e.getMessage(), e);
            throw new BadRequestException("Could not send the login code right now, please try again");
        }
    }

    public void verify(String email, String code) {
        // TEST ONLY: accept the fixed code for the designated test account, ignoring expiry.
        if (testLoginSupport.matches(email) && testLoginSupport.fixedOtp().equals(code)) {
            otpCodeRepository.findTopByEmailAndConsumedFalseOrderByIdDesc(email).ifPresent(existing -> {
                existing.setConsumed(true);
                otpCodeRepository.save(existing);
            });
            return;
        }
        OtpCode otp = otpCodeRepository.findTopByEmailAndConsumedFalseOrderByIdDesc(email)
                .orElseThrow(() -> new BadRequestException("No OTP was requested for this email"));

        if (otp.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("OTP has expired, please request a new one");
        }
        if (!otp.getCode().equals(code)) {
            throw new BadRequestException("Incorrect OTP");
        }

        otp.setConsumed(true);
        otpCodeRepository.save(otp);
    }
}
