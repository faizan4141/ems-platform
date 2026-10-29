package com.ems.academic.service;

import com.ems.academic.domain.TenantSnapshot;
import com.ems.academic.event.TenantEvent;
import com.ems.academic.repository.TenantSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantSnapshotServiceTest {

    @Mock
    TenantSnapshotRepository repository;
    @InjectMocks
    TenantSnapshotService service;

    private final UUID tenantId = UUID.randomUUID();
    private final Instant t0 = Instant.parse("2026-10-01T10:00:00Z");

    private TenantEvent event(String status, Instant at) {
        return new TenantEvent(UUID.randomUUID(), "TENANT_STATUS_CHANGED",
                tenantId, "dps", "DPS", status, at);
    }

    @Test
    void apply_createsSnapshotForNewTenant() {
        when(repository.findById(tenantId)).thenReturn(Optional.empty());

        service.apply(event("ACTIVE", t0));

        verify(repository).save(any(TenantSnapshot.class));
    }

    @Test
    void apply_ignoresOlderEvent() {
        TenantSnapshot existing = new TenantSnapshot(tenantId);
        existing.update("dps", "DPS", "SUSPENDED", t0);
        when(repository.findById(tenantId)).thenReturn(Optional.of(existing));

        service.apply(event("ACTIVE", t0.minusSeconds(60)));

        assertThat(existing.getStatus()).isEqualTo("SUSPENDED");
        verify(repository, never()).save(any());
    }

    @Test
    void apply_appliesNewerEvent() {
        TenantSnapshot existing = new TenantSnapshot(tenantId);
        existing.update("dps", "DPS", "ACTIVE", t0);
        when(repository.findById(tenantId)).thenReturn(Optional.of(existing));

        service.apply(event("SUSPENDED", t0.plusSeconds(60)));

        assertThat(existing.getStatus()).isEqualTo("SUSPENDED");
        verify(repository).save(existing);
    }
}