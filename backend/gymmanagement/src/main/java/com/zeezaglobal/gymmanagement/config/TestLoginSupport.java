package com.zeezaglobal.gymmanagement.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TEST ONLY: a single designated email signs in with a fixed OTP, so QA / Play Store review can get into the
 * app without a real inbox. Disabled unless {@code app.test-login.otp} is set to a 6-digit code, so it is never
 * active in production by default. It is scoped to one email ({@code app.test-login.email}) and never affects
 * any other account.
 */
@Slf4j
@Component
public class TestLoginSupport {

    @Value("${app.test-login.email:}")
    private String email;

    @Value("${app.test-login.otp:}")
    private String otp;

    /** On only when a 6-digit fixed code and an email are both configured. */
    public boolean isEnabled() {
        return email != null && !email.isBlank() && otp != null && otp.matches("\\d{6}");
    }

    public boolean matches(String candidateEmail) {
        return isEnabled() && candidateEmail != null && email.trim().equalsIgnoreCase(candidateEmail.trim());
    }

    public String normalizedEmail() {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public String fixedOtp() {
        return otp;
    }
}
