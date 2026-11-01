package com.ems.academic.web;

import com.ems.academic.payment.WebhookEvent;
import com.ems.academic.service.PaymentWebhookService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentWebhookController {

    private final PaymentWebhookService service;

    public PaymentWebhookController(PaymentWebhookService service) { this.service = service; }

    /** Called by the payment provider, not by a logged-in user: it is authenticated by its signature. */
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> webhook(@RequestHeader(value = "X-Signature", required = false) String signature,
                                        @RequestParam String eventId, @RequestParam String type,
                                        @RequestParam String providerRef,
                                        @RequestParam(required = false) String providerPaymentId,
                                        @RequestParam String amount, @RequestParam String currency,
                                        @RequestParam(required = false) String failureReason) {
        service.handle(new WebhookEvent(eventId, type, providerRef, providerPaymentId, amount, currency, failureReason),
                signature);
        return ResponseEntity.ok().build();
    }
}