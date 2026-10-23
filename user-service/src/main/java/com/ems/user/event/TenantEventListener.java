package com.ems.user.event;

import com.ems.user.service.TenantSnapshotService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TenantEventListener {

    private final TenantSnapshotService snapshotService;

    public TenantEventListener(TenantSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @KafkaListener(topics = "ems.tenant.events")
    public void onTenantEvent(TenantEvent event) {
        snapshotService.apply(event);
    }
}