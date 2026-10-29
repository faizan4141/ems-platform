package com.ems.academic.event;

import java.time.Instant;
import java.util.UUID;

public record TenantEvent(
        UUID eventId,
        String eventType,
        UUID tenantId,
        String code,
        String name,
        String status,
        Instant occurredAt) {
}