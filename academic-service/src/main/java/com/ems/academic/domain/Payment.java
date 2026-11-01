package com.ems.academic.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "charge_id", nullable = false, updatable = false) private UUID chargeId;
    @Column(nullable = false, precision = 12, scale = 2, updatable = false) private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20, updatable = false) private PaymentMethod method;
    @Column(length = 100, updatable = false) private String reference;
    @Column(name = "paid_on", nullable = false, updatable = false) private LocalDate paidOn;
    @Column(name = "receipt_no", nullable = false, length = 30, updatable = false) private String receiptNo;
    @Column(name = "recorded_by", nullable = false, length = 100, updatable = false) private String recordedBy;
    @Column(name = "recorded_at", nullable = false, updatable = false) private Instant recordedAt;
    @Column(name = "idempotency_key", nullable = false, updatable = false) private UUID idempotencyKey;
    @Column(nullable = false) private boolean voided;
    @Column(name = "void_reason", length = 200) private String voidReason;
    @Column(name = "voided_by", length = 100) private String voidedBy;
    @Column(name = "voided_at") private Instant voidedAt;

    protected Payment() {}

    public Payment(UUID tenantId, UUID chargeId, BigDecimal amount, PaymentMethod method, String reference,
                   LocalDate paidOn, String receiptNo, String recordedBy, Instant recordedAt, UUID idempotencyKey) {
        this.tenantId = tenantId;
        this.chargeId = chargeId;
        this.amount = amount;
        this.method = method;
        this.reference = reference;
        this.paidOn = paidOn;
        this.receiptNo = receiptNo;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
        this.idempotencyKey = idempotencyKey;
    }

    public void voidIt(String reason, String actor, Instant now) {
        this.voided = true;
        this.voidReason = reason;
        this.voidedBy = actor;
        this.voidedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getChargeId() { return chargeId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentMethod getMethod() { return method; }
    public String getReference() { return reference; }
    public LocalDate getPaidOn() { return paidOn; }
    public String getReceiptNo() { return receiptNo; }
    public boolean isVoided() { return voided; }
    public String getVoidReason() { return voidReason; }
}