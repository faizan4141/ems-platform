package com.ems.academic.payment;

import com.ems.academic.config.PaymentConfig.PaymentSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Development stand-in: the "checkout page" is part of our own app. */
@Component
@ConditionalOnProperty(name = "ems.payments.provider", havingValue = "simulated", matchIfMissing = true)
public class SimulatedPaymentGateway implements PaymentGateway {

    private final PaymentSettings settings;

    public SimulatedPaymentGateway(PaymentSettings settings) { this.settings = settings; }

    @Override
    public String name() { return "simulated"; }

    @Override
    public CheckoutSession createCheckout(CheckoutRequest request) {
        String ref = "sim_" + UUID.randomUUID().toString().replace("-", "");
        String base = settings.appBaseUrl().replaceAll("/+$", "");
        return new CheckoutSession(ref, base + "/pay/simulated/" + ref + "?attempt=" + request.attemptId());
    }

    @Override
    public boolean verifySignature(WebhookEvent event, String signature) {
        return WebhookSigner.verify(settings.webhookSecret(), event, signature);
    }

    /** Only the simulated checkout needs to sign events itself. */
    public String sign(WebhookEvent event) { return WebhookSigner.sign(settings.webhookSecret(), event); }
}