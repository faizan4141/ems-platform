package com.ems.academic.service;

import com.ems.academic.domain.TenantSnapshot;
import com.ems.academic.exception.TenantNotActiveException;
import com.ems.academic.exception.TenantNotFoundException;
import com.ems.academic.repository.TenantSnapshotRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantGuard {

    private final TenantSnapshotRepository snapshots;

    public TenantGuard(TenantSnapshotRepository snapshots) { this.snapshots = snapshots; }

    public TenantSnapshot requireActive(UUID tenantId) {
        TenantSnapshot tenant = snapshots.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new TenantNotActiveException(tenantId);
        }
        return tenant;
    }
}