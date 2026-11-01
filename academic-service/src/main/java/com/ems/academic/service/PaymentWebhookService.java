package com.ems.academic.service;

import com.ems.academic.domain.*;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.InvalidSignatureException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.payment.PaymentGateway;
import com.ems.academic.payment.WebhookEvent;
import com.ems.academic.repository.FeeChargeRepository;
import com.ems.academic.repository.PaymentAttemptItemRepository;
import com.ems.academic.repository.PaymentAttemptRepository;
import com.ems.academic.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookService.class);

    private final PaymentAttemptRepository attempts;
    private final PaymentAttemptItemRepository items;
    private final FeeChargeRepository charges;
    private final PaymentRepository payments;
    private final ReceiptNumbers receipts;
    private final PaymentGateway gateway;
    private final Clock clock;

    public PaymentWebhookService(PaymentAttemptRepository attempts, PaymentAttemptItemRepository items,
                                 FeeChargeRepository charges, PaymentRepository payments, ReceiptNumbers receipts,
                                 PaymentGateway gateway, Clock clock) {
        this.attempts = attempts;
        this.items = items;
        this.charges = charges;
        this.payments = payments;
        this.receipts = receipts;
        this.gateway = gateway;
        this.clock = clock;
    }

    @Transactional
    public void handle(WebhookEvent event, String signature) {
        // Authenticity first: until this passes, we do not even look anything up.
        if (!gateway.verifySignature(event, signature)) {
            throw new InvalidSignatureException();
        }
        PaymentAttempt attempt = attempts.lockByProviderRef(event.providerRef())
                .orElseThrow(() -> new ResourceNotFoundException("Unknown payment reference"));

        switch (event.type()) {
            case WebhookEvent.SUCCEEDED -> onSuccess(attempt, event);
            case WebhookEvent.FAILED -> onFailure(attempt, event);
            default -> throw new InvalidRequestException("Unknown event type");
        }
    }

    private void onFailure(PaymentAttempt attempt, WebhookEvent event) {
        if (attempt.getStatus() == AttemptStatus.PENDING) {
            attempt.fail(blank(event.failureReason()) ? "The payment was not completed" : event.failureReason(), clock.instant());
        } else {
            log.info("Ignoring a failure event for attempt {} that is already {}", attempt.getId(), attempt.getStatus());
        }
    }

    private void onSuccess(PaymentAttempt attempt, WebhookEvent event) {
        if (attempt.getStatus() == AttemptStatus.SUCCEEDED || attempt.getStatus() == AttemptStatus.REVIEW) {
            return;   // a repeated delivery: already handled
        }
        Instant now = clock.instant();
        BigDecimal reported = parse(event.amount());
        if (reported == null || reported.compareTo(attempt.getAmount()) != 0
                || !attempt.getCurrency().equalsIgnoreCase(String.valueOf(event.currency()))) {
            attempt.review("The provider reported " + event.amount() + " " + event.currency() + " but "
                    + attempt.getAmount().toPlainString() + " " + attempt.getCurrency()
                    + " was expected. Nothing was applied.", now);
            return;
        }

        UUID tenantId = attempt.getTenantId();
        LocalDate today = LocalDate.now(clock);
        String reference = blank(event.providerPaymentId()) ? event.providerRef() : event.providerPaymentId();
        List<PaymentAttemptItem> lines = items.findByAttemptId(attempt.getId()).stream()
                .sorted(Comparator.comparing(PaymentAttemptItem::getChargeId)).toList();   // fixed lock order

        BigDecimal unapplied = FeeMath.ZERO;
        for (PaymentAttemptItem line : lines) {
            UUID chargeId = line.getChargeId();
            charges.lockById(chargeId, tenantId).orElseThrow(() -> new ResourceNotFoundException("Fee not found"));

            // Derived from attempt + fee: even a racing duplicate cannot create a second receipt.
            UUID key = UUID.nameUUIDFromBytes((attempt.getId() + ":" + chargeId).getBytes(StandardCharsets.UTF_8));
            if (payments.findByTenantIdAndIdempotencyKey(tenantId, key).isPresent()) {
                continue;
            }
            BigDecimal paid = payments.sumForCharge(chargeId);
            BigDecimal balance = FeeMath.balance(chargeAmount(chargeId, line), paid == null ? FeeMath.ZERO : paid);
            BigDecimal apply = line.getAmount().min(balance);
            if (apply.signum() > 0) {
                Payment payment = new Payment(tenantId, chargeId, apply, PaymentMethod.ONLINE,
                        reference.length() > 100 ? reference.substring(0, 100) : reference, today,
                        receipts.next(tenantId), "online", now, key);
                payment.forAttempt(attempt.getId());
                payments.save(payment);
            }
            unapplied = unapplied.add(line.getAmount().subtract(apply));
        }

        if (unapplied.signum() > 0) {
            attempt.review(unapplied.toPlainString() + " " + attempt.getCurrency()
                    + " could not be applied because those fees were paid in the meantime. Please refund it.", now);
        } else {
            attempt.succeed(now);
        }
    }

    /** The charge's own amount is what caps the balance, not the attempt line. */
    private BigDecimal chargeAmount(UUID chargeId, PaymentAttemptItem line) {
        return charges.findById(chargeId).map(FeeCharge::getAmount).orElse(line.getAmount());
    }

    private static BigDecimal parse(String text) {
        try {
            return text == null ? null : new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}