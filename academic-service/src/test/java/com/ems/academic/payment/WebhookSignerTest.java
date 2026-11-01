package com.ems.academic.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookSignerTest {

    private final WebhookEvent event = new WebhookEvent("evt-1", "payment.succeeded", "sim_abc", "pay_1",
            "150.00", "INR", "");

    @Test
    void aGenuineSignatureVerifies() {
        assertThat(WebhookSigner.verify("secret", event, WebhookSigner.sign("secret", event))).isTrue();
    }

    @Test
    void anyChangedFieldBreaksTheSignature() {
        String signature = WebhookSigner.sign("secret", event);
        WebhookEvent tampered = new WebhookEvent("evt-1", "payment.succeeded", "sim_abc", "pay_1", "1.00", "INR", "");
        assertThat(WebhookSigner.verify("secret", tampered, signature)).isFalse();
    }

    @Test
    void aWrongSecretOrMissingSignatureFails() {
        String signature = WebhookSigner.sign("secret", event);
        assertThat(WebhookSigner.verify("other-secret", event, signature)).isFalse();
        assertThat(WebhookSigner.verify("secret", event, null)).isFalse();
        assertThat(WebhookSigner.verify("secret", event, " ")).isFalse();
    }

    @Test
    void theSignatureIsCaseInsensitiveHex() {
        assertThat(WebhookSigner.verify("secret", event, WebhookSigner.sign("secret", event).toUpperCase())).isTrue();
    }
}