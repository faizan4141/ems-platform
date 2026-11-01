package com.ems.academic.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fee_items")
public class FeeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "class_id", nullable = false, updatable = false) private UUID classId;
    @Column(nullable = false, length = 100) private String name;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(name = "due_date", nullable = false) private LocalDate dueDate;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected FeeItem() {}

    public FeeItem(UUID tenantId, UUID classId, String name, BigDecimal amount, LocalDate dueDate) {
        this.tenantId = tenantId;
        this.classId = classId;
        this.name = name;
        this.amount = amount;
        this.dueDate = dueDate;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getClassId() { return classId; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
}