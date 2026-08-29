package com.zeezaglobal.gymmanagement.payment;

import com.zeezaglobal.gymmanagement.entity.MemberPaymentStatus;

/**
 * Result of asking a gateway to start a charge. {@code checkoutUrl}/{@code clientSecret} is whichever
 * the provider's client-side SDK needs to complete the payment (e.g. Stripe PaymentIntent client secret,
 * Razorpay order id, a UPI intent/QR payload) — left as a generic string until a real provider is wired in.
 */
public record PaymentGatewayResult(
        String referenceId,
        String clientSecretOrCheckoutPayload,
        MemberPaymentStatus status
) {
}
