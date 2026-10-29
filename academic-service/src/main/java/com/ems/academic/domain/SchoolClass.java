package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "classes")
public class SchoolClass {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "academic_year_id", nullable = false, updatable = false) private UUID academicYearId;
    @Column(nullable = false, length = 50) private String name;
    @Column(name = "grade_level", nullable = false) private int gradeLevel;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected SchoolClass() {}

    public SchoolClass(UUID tenantId, UUID academicYearId, String name, int gradeLevel) {
        this.tenantId = tenantId;
        this.academicYearId = academicYearId;
        this.name = name;
        this.gradeLevel = gradeLevel;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getAcademicYearId() { return academicYearId; }
    public String getName() { return name; }
    public int getGradeLevel() { return gradeLevel; }
}