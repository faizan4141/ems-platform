package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "academic_years")
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(nullable = false, length = 50) private String name;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Column(name = "is_current", nullable = false) private boolean currentYear;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected AcademicYear() {}

    public AcademicYear(UUID tenantId, String name, LocalDate startDate, LocalDate endDate, boolean currentYear) {
        this.tenantId = tenantId;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.currentYear = currentYear;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isCurrentYear() { return currentYear; }
    public void setCurrentYear(boolean currentYear) { this.currentYear = currentYear; }
}