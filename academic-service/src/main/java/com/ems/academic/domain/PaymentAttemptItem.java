package com.ems.academic.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payment_attempt_items")
public class PaymentAttemptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "attempt_id", nullable = false, updatable = false) private UUID attemptId;
    @Column(name = "charge_id", nullable = false, updatable = false) private UUID chargeId;
    @Column(nullable = false, precision = 12, scale = 2, updatable = false) private BigDecimal amount;

    protected PaymentAttemptItem() {}

    public PaymentAttemptItem(UUID attemptId, UUID chargeId, BigDecimal amount) {
        this.attemptId = attemptId;
        this.chargeId = chargeId;
        this.amount = amount;
    }

    public UUID getAttemptId() { return attemptId; }
    public UUID getChargeId() { return chargeId; }
    public BigDecimal getAmount() { return amount; }
}