package com.ems.academic.service;

import com.ems.academic.domain.TenantSnapshot;
import com.ems.academic.event.TenantEvent;
import com.ems.academic.repository.TenantSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(TenantSnapshotService.class);
    private final TenantSnapshotRepository repository;

    public TenantSnapshotService(TenantSnapshotRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void apply(TenantEvent event) {
        TenantSnapshot snapshot = repository.findById(event.tenantId())
                .orElseGet(() -> new TenantSnapshot(event.tenantId()));

        // Idempotency: a duplicate or older event must never overwrite newer data
        if (snapshot.getLastEventAt() != null
                && event.occurredAt().isBefore(snapshot.getLastEventAt())) {
            log.info("Ignoring stale {} for tenant {}", event.eventType(), event.tenantId());
            return;
        }

        snapshot.update(event.code(), event.name(), event.status(), event.occurredAt());
        repository.save(snapshot);
        log.info("Applied {} for tenant {} (status={})",
                event.eventType(), event.tenantId(), event.status());
    }
}