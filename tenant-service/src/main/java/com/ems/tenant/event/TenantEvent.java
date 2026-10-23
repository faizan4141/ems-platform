package com.ems.tenant.event;

import com.ems.tenant.domain.Tenant;

import java.time.Instant;
import java.util.UUID;

public record TenantEvent(
        UUID eventId,
        String eventType,        // TENANT_CREATED or TENANT_STATUS_CHANGED
        UUID tenantId,
        String code,
        String name,
        String status,
        Instant occurredAt) {

    public static final String CREATED = "TENANT_CREATED";
    public static final String STATUS_CHANGED = "TENANT_STATUS_CHANGED";

    public static TenantEvent of(String type, Tenant t) {
        return new TenantEvent(UUID.randomUUID(), type, t.getId(), t.getCode(),
                t.getName(), t.getStatus().name(), Instant.now());
    }
}