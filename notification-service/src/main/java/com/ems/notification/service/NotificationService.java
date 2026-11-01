package com.ems.notification.service;

import com.ems.notification.domain.Channel;
import com.ems.notification.domain.Notification;
import com.ems.notification.event.AttendanceAlertEvent;
import com.ems.notification.event.FeeReminderEvent;
import com.ems.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final Clock clock;
    private final String brand;

    public NotificationService(NotificationRepository repository, Clock clock,
                               @Value("${notifications.brand}") String brand) {
        this.repository = repository;
        this.clock = clock;
        this.brand = brand;
    }

    /** Stores the messages for an event. Kafka delivers at least once, so a repeated event creates nothing. */
    @Transactional
    public int handle(AttendanceAlertEvent event) {
        if (repository.existsByEventId(event.eventId())) {
            return 0;
        }
        return store(event.tenantId(), event.eventId(), event.guardianEmail(), event.guardianPhone(),
                MessageTemplates.attendance(event, brand));
    }

    @Transactional
    public int handleFee(FeeReminderEvent event) {
        if (repository.existsByEventId(event.eventId())) {
            return 0;
        }
        return store(event.tenantId(), event.eventId(), event.guardianEmail(), event.guardianPhone(),
                MessageTemplates.feeReminder(event, brand));
    }

    private int store(UUID tenantId, UUID eventId, String email, String phone, MessageTemplates.Message message) {
        Instant now = clock.instant();
        List<Notification> rows = new ArrayList<>();
        if (email != null && !email.isBlank()) {
            rows.add(new Notification(tenantId, eventId, Channel.EMAIL, email, message.subject(), message.body(), now));
        }
        rows.add(new Notification(tenantId, eventId, Channel.SMS, phone, null, message.body(), now));
        repository.saveAll(rows);
        return rows.size();
    }
}