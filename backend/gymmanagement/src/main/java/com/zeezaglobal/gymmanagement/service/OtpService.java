package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.entity.OtpCode;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.repository.OtpCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public void generate(String email) {
        String code = String.format("%06d", random.nextInt(1_000_000));

        OtpCode otp = new OtpCode();
        otp.setEmail(email);
        otp.setCode(code);
        otp.setExpiresAt(Instant.now().plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES));
        otpCodeRepository.save(otp);

        log.info("OTP generated for {}: {} (expires in {} minutes)", email, code, OTP_TTL_MINUTES);
        emailService.sendOtpEmail(email, code);
    }

    public void verify(String email, String code) {
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
