package com.ems.academic.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class WebhookSigner {

    private WebhookSigner() {}

    static String canonical(WebhookEvent e) {
        return String.join("|", nz(e.eventId()), nz(e.type()), nz(e.providerRef()), nz(e.providerPaymentId()),
                nz(e.amount()), nz(e.currency()), nz(e.failureReason()));
    }

    public static String sign(String secret, WebhookEvent event) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical(event).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    /** Constant-time comparison, so response timing reveals nothing about the expected signature. */
    public static boolean verify(String secret, WebhookEvent event, String signature) {
        if (signature == null || signature.isBlank()) return false;
        byte[] expected = sign(secret, event).getBytes(StandardCharsets.UTF_8);
        byte[] actual = signature.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private static String nz(String value) { return value == null ? "" : value; }
}