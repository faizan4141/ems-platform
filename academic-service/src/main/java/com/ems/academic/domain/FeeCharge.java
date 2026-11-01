package com.ems.academic.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fee_charges")
public class FeeCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "student_id", nullable = false, updatable = false) private UUID studentId;
    @Column(name = "fee_item_id", nullable = false, updatable = false) private UUID feeItemId;
    @Column(nullable = false, length = 100, updatable = false) private String name;
    @Column(nullable = false, precision = 12, scale = 2, updatable = false) private BigDecimal amount;
    @Column(name = "due_date", nullable = false, updatable = false) private LocalDate dueDate;
    @Column(name = "last_reminded_at") private Instant lastRemindedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected FeeCharge() {}

    public FeeCharge(UUID tenantId, UUID studentId, UUID feeItemId, String name, BigDecimal amount, LocalDate dueDate) {
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.feeItemId = feeItemId;
        this.name = name;
        this.amount = amount;
        this.dueDate = dueDate;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public void markReminded(Instant now) { this.lastRemindedAt = now; }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public UUID getFeeItemId() { return feeItemId; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
}