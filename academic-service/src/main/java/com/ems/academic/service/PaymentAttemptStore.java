package com.ems.academic.service;

import com.ems.academic.config.FeeConfig.FeeSettings;
import com.ems.academic.config.PaymentConfig.PaymentSettings;
import com.ems.academic.domain.FeeCharge;
import com.ems.academic.domain.PaymentAttempt;
import com.ems.academic.domain.PaymentAttemptItem;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.FeeChargeRepository;
import com.ems.academic.repository.PaymentAttemptItemRepository;
import com.ems.academic.repository.PaymentAttemptRepository;
import com.ems.academic.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentAttemptStore {

    public record StartResult(PaymentAttempt attempt, boolean existing, String description) {}

    private final PaymentAttemptRepository attempts;
    private final PaymentAttemptItemRepository items;
    private final FeeChargeRepository charges;
    private final PaymentRepository payments;
    private final Clock clock;
    private final FeeSettings feeSettings;
    private final PaymentSettings paymentSettings;

    public PaymentAttemptStore(PaymentAttemptRepository attempts, PaymentAttemptItemRepository items,
                               FeeChargeRepository charges, PaymentRepository payments, Clock clock,
                               FeeSettings feeSettings, PaymentSettings paymentSettings) {
        this.attempts = attempts;
        this.items = items;
        this.charges = charges;
        this.payments = payments;
        this.clock = clock;
        this.feeSettings = feeSettings;
        this.paymentSettings = paymentSettings;
    }

    @Transactional
    public StartResult create(UUID tenantId, UUID studentId, String subject, List<UUID> chargeIds,
                              UUID key, String provider) {
        Optional<PaymentAttempt> prior = attempts.findByTenantIdAndIdempotencyKey(tenantId, key);
        if (prior.isPresent()) {
            PaymentAttempt p = prior.get();
            if (!p.getInitiatedBy().equals(subject) || !p.getStudentId().equals(studentId)) {
                throw new DuplicateResourceException("This request key was already used");
            }
            return new StartResult(p, true, "");
        }

        Set<UUID> unique = new LinkedHashSet<>(chargeIds);
        List<FeeCharge> found = charges.findAllById(unique);
        if (found.size() != unique.size() || found.stream().anyMatch(c -> !c.getStudentId().equals(studentId))) {
            throw new ResourceNotFoundException("Fee not found");
        }
        Map<UUID, BigDecimal> paid = FeeMath.toPaidMap(payments.sumByCharge(unique));

        Map<UUID, BigDecimal> toPay = new LinkedHashMap<>();
        BigDecimal total = FeeMath.ZERO;
        for (FeeCharge c : found) {
            BigDecimal balance = FeeMath.balance(c.getAmount(), FeeMath.paidOf(paid, c.getId()));
            if (balance.signum() == 0) {
                throw new InvalidRequestException(c.getName() + " is already paid");
            }
            toPay.put(c.getId(), balance);
            total = total.add(balance);
        }

        long inFlight = attempts.countPendingForCharges(
                clock.instant().minus(Duration.ofMinutes(paymentSettings.pendingWindowMinutes())), unique);
        if (inFlight > 0) {
            throw new DuplicateResourceException(
                    "A payment for one of these fees is already in progress. Please wait a few minutes and check again.");
        }

        PaymentAttempt attempt = attempts.save(new PaymentAttempt(tenantId, studentId, subject, total,
                feeSettings.currency(), provider, key, clock.instant()));
        toPay.forEach((chargeId, amount) -> items.save(new PaymentAttemptItem(attempt.getId(), chargeId, amount)));

        String description = "School fees: " + found.stream().map(FeeCharge::getName).collect(Collectors.joining(", "));
        return new StartResult(attempt, false, description.length() > 100 ? description.substring(0, 100) : description);
    }

    @Transactional
    public void attach(UUID attemptId, String providerRef, String checkoutUrl) {
        attempts.findById(attemptId).orElseThrow().attachCheckout(providerRef, checkoutUrl);
    }

    @Transactional
    public void failSetup(UUID attemptId, String reason) {
        attempts.findById(attemptId).ifPresent(a -> a.fail(reason, clock.instant()));
    }
}