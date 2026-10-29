package com.ems.academic.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_snapshots")
public class TenantSnapshot {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;

    protected TenantSnapshot() {
    }

    public TenantSnapshot(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public void update(String code, String name, String status, Instant eventAt) {
        this.code = code;
        this.name = name;
        this.status = status;
        this.lastEventAt = eventAt;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public Instant getLastEventAt() {
        return lastEventAt;
    }
}