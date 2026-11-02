package com.ems.academic.event;

import com.ems.academic.service.GuardianLoginService;
import com.ems.academic.service.StaffLoginService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserLoginListener {

    private final GuardianLoginService guardians;
    private final StaffLoginService staff;

    public UserLoginListener(GuardianLoginService guardians, StaffLoginService staff) {
        this.guardians = guardians;
        this.staff = staff;
    }

    @KafkaListener(topics = "ems.user.events",
            properties = "spring.json.value.default.type=com.ems.academic.event.UserLoginEvent")
    public void onUserEvent(UserLoginEvent event) {
        guardians.apply(event);
        staff.apply(event);
    }
}