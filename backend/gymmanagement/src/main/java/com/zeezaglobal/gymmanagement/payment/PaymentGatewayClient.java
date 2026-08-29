package com.zeezaglobal.gymmanagement.payment;

/**
 * One implementation per provider (Stripe, Razorpay, UPI) — {@link PaymentGatewayClientResolver}
 * picks the right one for a given {@link com.zeezaglobal.gymmanagement.entity.PaymentMethod}.
 * None of these are wired to a real SDK yet — see {@link StripeGatewayClient}, {@link RazorpayGatewayClient},
 * and {@link UpiGatewayClient} for where that integration goes.
 */
public interface PaymentGatewayClient {

    /** Which {@link com.zeezaglobal.gymmanagement.entity.PaymentMethod} this client handles. */
    com.zeezaglobal.gymmanagement.entity.PaymentMethod supportedMethod();

    /** Starts a charge with the provider and returns a reference to track/confirm it. */
    PaymentGatewayResult initiate(PaymentGatewayRequest request);
}
