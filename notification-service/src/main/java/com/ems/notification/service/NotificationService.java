package com.ems.notification.service;

import com.ems.notification.domain.Channel;
import com.ems.notification.domain.Notification;
import com.ems.notification.event.AttendanceAlertEvent;
import com.ems.notification.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
        MessageTemplates.Message message = MessageTemplates.attendance(event, brand);
        Instant now = clock.instant();

        List<Notification> rows = new ArrayList<>();
        if (event.guardianEmail() != null && !event.guardianEmail().isBlank()) {
            rows.add(new Notification(event.tenantId(), event.eventId(), Channel.EMAIL,
                    event.guardianEmail(), message.subject(), message.body(), now));
        }
        rows.add(new Notification(event.tenantId(), event.eventId(), Channel.SMS,
                event.guardianPhone(), null, message.body(), now));
        repository.saveAll(rows);
        return rows.size();
    }
}