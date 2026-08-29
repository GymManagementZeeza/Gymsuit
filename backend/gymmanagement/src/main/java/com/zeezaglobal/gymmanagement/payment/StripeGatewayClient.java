package com.zeezaglobal.gymmanagement.payment;

import com.zeezaglobal.gymmanagement.entity.MemberPaymentStatus;
import com.zeezaglobal.gymmanagement.entity.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * TODO: wire up the real Stripe SDK (PaymentIntents API) here. For now this only returns a
 * placeholder reference so the rest of the payment flow (recording, status tracking) can be
 * built and tested ahead of actual gateway integration.
 */
@Component
public class StripeGatewayClient implements PaymentGatewayClient {

    @Override
    public PaymentMethod supportedMethod() {
        return PaymentMethod.STRIPE;
    }

    @Override
    public PaymentGatewayResult initiate(PaymentGatewayRequest request) {
        String reference = "stub_stripe_" + UUID.randomUUID();
        return new PaymentGatewayResult(reference, null, MemberPaymentStatus.PENDING);
    }
}
