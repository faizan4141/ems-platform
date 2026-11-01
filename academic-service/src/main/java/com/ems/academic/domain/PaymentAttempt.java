package com.ems.academic.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "student_id", nullable = false, updatable = false) private UUID studentId;
    @Column(name = "initiated_by", nullable = false, length = 100, updatable = false) private String initiatedBy;
    @Column(nullable = false, precision = 12, scale = 2, updatable = false) private BigDecimal amount;
    @Column(nullable = false, length = 3, updatable = false) private String currency;
    @Column(nullable = false, length = 30, updatable = false) private String provider;
    @Column(name = "provider_ref", length = 100) private String providerRef;
    @Column(name = "checkout_url", length = 500) private String checkoutUrl;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private AttemptStatus status;
    @Column(name = "failure_reason", length = 200) private String failureReason;
    @Column(name = "review_note", length = 300) private String reviewNote;
    @Column(name = "idempotency_key", nullable = false, updatable = false) private UUID idempotencyKey;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "completed_at") private Instant completedAt;

    protected PaymentAttempt() {}

    public PaymentAttempt(UUID tenantId, UUID studentId, String initiatedBy, BigDecimal amount, String currency,
                          String provider, UUID idempotencyKey, Instant now) {
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.initiatedBy = initiatedBy;
        this.amount = amount;
        this.currency = currency;
        this.provider = provider;
        this.idempotencyKey = idempotencyKey;
        this.status = AttemptStatus.PENDING;
        this.createdAt = now;
    }

    public void attachCheckout(String providerRef, String checkoutUrl) {
        this.providerRef = providerRef;
        this.checkoutUrl = checkoutUrl;
    }

    public void succeed(Instant now) { this.status = AttemptStatus.SUCCEEDED; this.completedAt = now; }

    public void fail(String reason, Instant now) {
        this.status = AttemptStatus.FAILED;
        this.failureReason = cut(reason, 200);
        this.completedAt = now;
    }

    public void review(String note, Instant now) {
        this.status = AttemptStatus.REVIEW;
        this.reviewNote = cut(note, 300);
        this.completedAt = now;
    }

    private static String cut(String value, int max) {
        if (value == null) return null;
        return value.length() > max ? value.substring(0, max) : value;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getStudentId() { return studentId; }
    public String getInitiatedBy() { return initiatedBy; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getProvider() { return provider; }
    public String getProviderRef() { return providerRef; }
    public String getCheckoutUrl() { return checkoutUrl; }
    public AttemptStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    public String getReviewNote() { return reviewNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}