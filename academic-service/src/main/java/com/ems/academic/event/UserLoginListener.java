package com.ems.academic.event;

import com.ems.academic.service.GuardianLoginService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserLoginListener {

    private final GuardianLoginService service;

    public UserLoginListener(GuardianLoginService service) { this.service = service; }

    @KafkaListener(topics = "ems.user.events",
            properties = "spring.json.value.default.type=com.ems.academic.event.UserLoginEvent")
    public void onUserEvent(UserLoginEvent event) {
        service.apply(event);
    }
}