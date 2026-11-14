package com.zeezaglobal.gymmanagement.service;

import com.zeezaglobal.gymmanagement.config.TestLoginSupport;
import com.zeezaglobal.gymmanagement.entity.OtpCode;
import com.zeezaglobal.gymmanagement.exception.BadRequestException;
import com.zeezaglobal.gymmanagement.messaging.EventPublisher;
import com.zeezaglobal.gymmanagement.repository.OtpCodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    private static final String TEST_EMAIL = "athul.6560@gmail.com";
    private static final String FIXED = "123456";

    @Mock private OtpCodeRepository otpCodeRepository;
    @Mock private EventPublisher eventPublisher;

    private TestLoginSupport enabledSupport() {
        TestLoginSupport s = new TestLoginSupport();
        ReflectionTestUtils.setField(s, "email", TEST_EMAIL);
        ReflectionTestUtils.setField(s, "otp", FIXED);
        return s;
    }

    private TestLoginSupport disabledSupport() {
        TestLoginSupport s = new TestLoginSupport();
        ReflectionTestUtils.setField(s, "email", TEST_EMAIL);
        ReflectionTestUtils.setField(s, "otp", ""); // no code => disabled
        return s;
    }

    private OtpService service(TestLoginSupport support) {
        return new OtpService(otpCodeRepository, eventPublisher, support);
    }

    @Test
    void testAccountGetsFixedCodeAndNoEmail() {
        OtpService service = service(enabledSupport());
        service.generate(TEST_EMAIL);

        ArgumentCaptor<OtpCode> saved = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeRepository).save(saved.capture());
        assertEquals(FIXED, saved.getValue().getCode(), "test account always gets the fixed code");
        verify(eventPublisher, never()).publishEmail(anyString(), anyString(), anyString());
    }

    @Test
    void testAccountVerifiesWithFixedCodeIgnoringExpiry() {
        OtpService service = service(enabledSupport());
        when(otpCodeRepository.findTopByEmailAndConsumedFalseOrderByIdDesc(TEST_EMAIL)).thenReturn(Optional.empty());
        // Does not throw even with no stored/valid code.
        service.verify(TEST_EMAIL, FIXED);
        assertThrows(BadRequestException.class, () -> service.verify(TEST_EMAIL, "000000"),
                "a wrong code still falls through to the normal check");
    }

    @Test
    void normalAccountUnaffectedWhenEnabled() {
        OtpService service = service(enabledSupport());
        service.generate("someone@example.test");
        ArgumentCaptor<OtpCode> saved = ArgumentCaptor.forClass(OtpCode.class);
        verify(otpCodeRepository).save(saved.capture());
        assertEquals(6, saved.getValue().getCode().length());
        verify(eventPublisher).publishEmail(eq("someone@example.test"), anyString(), anyString());
    }

    @Test
    void testEmailGetsRandomCodeAndEmailWhenDisabled() {
        OtpService service = service(disabledSupport());
        service.generate(TEST_EMAIL);
        verify(eventPublisher).publishEmail(eq(TEST_EMAIL), anyString(), anyString());
    }

    @Test
    void verifyRejectsFixedCodeForTestEmailWhenDisabled() {
        OtpService service = service(disabledSupport());
        when(otpCodeRepository.findTopByEmailAndConsumedFalseOrderByIdDesc(TEST_EMAIL))
                .thenReturn(Optional.empty());
        assertThrows(BadRequestException.class, () -> service.verify(TEST_EMAIL, FIXED));
    }
}
