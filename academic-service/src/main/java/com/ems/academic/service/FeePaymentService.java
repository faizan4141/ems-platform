package com.ems.academic.service;

import com.ems.academic.domain.FeeCharge;
import com.ems.academic.domain.Payment;
import com.ems.academic.domain.PaymentMethod;
import com.ems.academic.dto.FeeDtos.PaymentRequest;
import com.ems.academic.dto.FeeDtos.PaymentView;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.FeeChargeRepository;
import com.ems.academic.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class FeePaymentService {

    private static final Set<PaymentMethod> NEEDS_REFERENCE =
            EnumSet.of(PaymentMethod.CHEQUE, PaymentMethod.UPI, PaymentMethod.BANK_TRANSFER);

    private final FeeChargeRepository charges;
    private final PaymentRepository payments;
    private final ReceiptNumbers receipts;
    private final TenantGuard guard;
    private final Clock clock;

    public FeePaymentService(FeeChargeRepository charges, PaymentRepository payments, ReceiptNumbers receipts,
                             TenantGuard guard, Clock clock) {
        this.charges = charges;
        this.payments = payments;
        this.receipts = receipts;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional
    public PaymentView record(UUID tenantId, UUID chargeId, PaymentRequest req, String actor) {
        guard.requireActive(tenantId);
        LocalDate today = LocalDate.now(clock);
        LocalDate paidOn = req.paidOn() == null ? today : req.paidOn();
        if (paidOn.isAfter(today)) {
            throw new InvalidRequestException("The payment date cannot be in the future");
        }
        String reference = clean(req.reference());
        if (reference == null && NEEDS_REFERENCE.contains(req.method())) {
            throw new InvalidRequestException("A reference is needed for "
                    + req.method().name().toLowerCase(Locale.ROOT).replace('_', ' ') + " payments");
        }

        FeeCharge charge = charges.lockById(chargeId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee not found"));

        // A retried submission returns the original receipt instead of taking the money twice.
        Optional<Payment> prior = payments.findByTenantIdAndIdempotencyKey(tenantId, req.idempotencyKey());
        if (prior.isPresent()) {
            if (!prior.get().getChargeId().equals(chargeId)) {
                throw new DuplicateResourceException("This request key was already used for another payment");
            }
            return PaymentView.from(prior.get(), charge.getName());
        }

        BigDecimal amount = FeeMath.scale(req.amount());
        BigDecimal paid = orZero(payments.sumForCharge(chargeId));
        BigDecimal balance = FeeMath.balance(charge.getAmount(), paid);
        if (amount.compareTo(balance) > 0) {
            throw new InvalidRequestException("The amount is more than the remaining balance of " + balance.toPlainString());
        }
        Payment saved = payments.save(new Payment(tenantId, chargeId, amount, req.method(), reference, paidOn,
                receipts.next(tenantId), actor, clock.instant(), req.idempotencyKey()));
        return PaymentView.from(saved, charge.getName());
    }

    @Transactional
    public PaymentView voidPayment(UUID tenantId, UUID paymentId, String reason, String actor) {
        guard.requireActive(tenantId);
        Payment payment = payments.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        // Same lock as recording, so a void and a new payment on one fee never interleave.
        FeeCharge charge = charges.lockById(payment.getChargeId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee not found"));
        if (payment.isVoided()) {
            throw new DuplicateResourceException("This payment is already voided");
        }
        payment.voidIt(reason.trim(), actor, clock.instant());
        return PaymentView.from(payment, charge.getName());
    }

    private static BigDecimal orZero(BigDecimal value) { return value == null ? FeeMath.ZERO : value; }

    private static String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}