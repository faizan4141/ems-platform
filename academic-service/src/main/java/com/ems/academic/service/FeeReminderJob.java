package com.ems.academic.service;

import com.ems.academic.domain.TenantSnapshot;
import com.ems.academic.repository.TenantSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "ems.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class FeeReminderJob {

    private static final Logger log = LoggerFactory.getLogger(FeeReminderJob.class);

    private final TenantSnapshotRepository tenants;
    private final FeeReminderService reminders;

    public FeeReminderJob(TenantSnapshotRepository tenants, FeeReminderService reminders) {
        this.tenants = tenants;
        this.reminders = reminders;
    }

    // Run ONE instance of this service until the job is made cluster-safe; two instances would remind twice.
    @Scheduled(cron = "${ems.fees.reminder-cron:0 0 9 * * *}", zone = "${ems.timezone:Asia/Kolkata}")
    public void run() {
        for (TenantSnapshot tenant : tenants.findByStatus("ACTIVE")) {
            try {
                int sent = reminders.remindTenant(tenant.getTenantId());
                if (sent > 0) {
                    log.info("Sent {} fee reminders for tenant {}", sent, tenant.getTenantId());
                }
            } catch (RuntimeException e) {
                log.error("Fee reminders failed for tenant {}", tenant.getTenantId(), e);
            }
        }
    }
}