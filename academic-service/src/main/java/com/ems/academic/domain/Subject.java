package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 20) private String code;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected Subject() {}

    public Subject(UUID tenantId, String name, String code) {
        this.tenantId = tenantId;
        this.name = name;
        this.code = code;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getCode() { return code; }
}