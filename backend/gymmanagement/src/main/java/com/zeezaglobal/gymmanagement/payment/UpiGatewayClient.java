package com.zeezaglobal.gymmanagement.payment;

import com.zeezaglobal.gymmanagement.entity.MemberPaymentStatus;
import com.zeezaglobal.gymmanagement.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * TODO: wire up a real UPI intent/QR provider here (e.g. via Razorpay's UPI methods, or a
 * dedicated UPI PSP). For now this only returns a placeholder reference so the rest of the
 * payment flow (recording, status tracking) can be built and tested ahead of actual integration.
 */
@Component
public class UpiGatewayClient implements PaymentGatewayClient {

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.UPI;
    }

    @Override
    public PaymentGatewayResult initiate(PaymentGatewayRequest request) {
        String reference = "stub_upi_" + UUID.randomUUID();
        return new PaymentGatewayResult(reference, null, MemberPaymentStatus.PENDING);
    }
}
