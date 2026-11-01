package com.ems.academic.payment;

/** What a provider tells us. Amount is the provider's own text, so it is signed exactly as received. */
public record WebhookEvent(String eventId, String type, String providerRef, String providerPaymentId,
                           String amount, String currency, String failureReason) {

    public static final String SUCCEEDED = "payment.succeeded";
    public static final String FAILED = "payment.failed";
}