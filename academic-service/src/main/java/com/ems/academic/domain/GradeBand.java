package com.ems.academic.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "grade_bands")
public class GradeBand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "min_percent", nullable = false, precision = 5, scale = 2) private BigDecimal minPercent;
    @Column(nullable = false, length = 10) private String grade;
    @Column(length = 50) private String label;

    protected GradeBand() {}

    public GradeBand(UUID tenantId, BigDecimal minPercent, String grade, String label) {
        this.tenantId = tenantId;
        this.minPercent = minPercent;
        this.grade = grade;
        this.label = label;
    }

    public BigDecimal getMinPercent() { return minPercent; }
    public String getGrade() { return grade; }
    public String getLabel() { return label; }
}