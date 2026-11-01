package com.ems.academic.web;

import com.ems.academic.domain.PaymentAttempt;
import com.ems.academic.dto.OnlinePaymentDtos.AttemptView;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.payment.SimulatedPaymentGateway;
import com.ems.academic.payment.WebhookEvent;
import com.ems.academic.repository.PaymentAttemptRepository;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.PaymentWebhookService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments/simulated")
@ConditionalOnProperty(name = "ems.payments.provider", havingValue = "simulated", matchIfMissing = true)
public class SimulatedCheckoutController {

    private final PaymentAttemptRepository attempts;
    private final PaymentWebhookService webhook;
    private final SimulatedPaymentGateway gateway;

    public SimulatedCheckoutController(PaymentAttemptRepository attempts, PaymentWebhookService webhook,
                                       SimulatedPaymentGateway gateway) {
        this.attempts = attempts;
        this.webhook = webhook;
        this.gateway = gateway;
    }

    /** Plays the provider: sends a signed webhook through the very same code a real provider would hit. */
    @PostMapping("/{providerRef}/complete")
    public AttemptView complete(@AuthenticationPrincipal Jwt jwt, @PathVariable String providerRef,
                                @RequestParam String outcome) {
        UUID tenantId = TenantClaims.requireTenantId(jwt);
        PaymentAttempt attempt = attempts.findByProviderRef(providerRef)
                .filter(a -> a.getTenantId().equals(tenantId) && a.getInitiatedBy().equals(jwt.getSubject()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        boolean success = switch (outcome) {
            case "success" -> true;
            case "failure" -> false;
            default -> throw new InvalidRequestException("outcome must be success or failure");
        };

        WebhookEvent event = new WebhookEvent(UUID.randomUUID().toString(),
                success ? WebhookEvent.SUCCEEDED : WebhookEvent.FAILED, providerRef,
                success ? "pay_" + UUID.randomUUID().toString().replace("-", "") : "",
                attempt.getAmount().toPlainString(), attempt.getCurrency(), success ? "" : "Simulated failure");
        webhook.handle(event, gateway.sign(event));

        return AttemptView.from(attempts.findById(attempt.getId()).orElseThrow());
    }
}