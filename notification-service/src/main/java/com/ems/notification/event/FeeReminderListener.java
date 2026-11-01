package com.ems.notification.event;

import com.ems.notification.service.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FeeReminderListener {

    private final NotificationService service;

    public FeeReminderListener(NotificationService service) { this.service = service; }

    @KafkaListener(topics = "ems.fee.events",
            properties = "spring.json.value.default.type=com.ems.notification.event.FeeReminderEvent")
    public void onFeeReminder(FeeReminderEvent event) {
        service.handleFee(event);
    }
}