package com.ems.academic.payment;

public interface PaymentGateway {

    String name();

    /** Creates a hosted checkout for the amount and returns where to send the parent. */
    CheckoutSession createCheckout(CheckoutRequest request);

    /** True only if the provider really sent this event. */
    boolean verifySignature(WebhookEvent event, String signature);
}