package com.ems.notification.event;

import com.ems.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AttendanceAlertListener {

    private final NotificationService service;

    public AttendanceAlertListener(NotificationService service) { this.service = service; }

    @KafkaListener(topics = "ems.attendance.events")
    public void onAlert(AttendanceAlertEvent event) {
        service.handle(event);
    }
}