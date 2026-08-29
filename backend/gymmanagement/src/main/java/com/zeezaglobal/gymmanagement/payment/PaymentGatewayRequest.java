package com.zeezaglobal.gymmanagement.payment;

import java.math.BigDecimal;

/**
 * What a gateway client needs to start a charge. Deliberately provider-agnostic — a real
 * Stripe/Razorpay/UPI implementation maps these onto its own SDK's request shape.
 */
public record PaymentGatewayRequest(
        Long gymId,
        Long memberId,
        Long memberPaymentId,
        BigDecimal amount,
        String currency,
        String description
) {
}
