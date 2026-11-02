package com.ems.academic.service;

import com.ems.academic.domain.StaffMember;
import com.ems.academic.event.UserLoginEvent;
import com.ems.academic.repository.StaffMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class StaffLoginService {

    private static final Logger log = LoggerFactory.getLogger(StaffLoginService.class);
    private final StaffMemberRepository staff;

    public StaffLoginService(StaffMemberRepository staff) { this.staff = staff; }

    /** Safe to repeat: the login id is unique, so a redelivered event just refreshes the same record. */
    @Transactional
    public void apply(UserLoginEvent event) {
        if (!UserLoginEvent.TEACHER_LOGIN_CREATED.equals(event.eventType())) {
            return;
        }
        if (event.keycloakId() == null || event.tenantId() == null
                || blank(event.email()) || blank(event.firstName()) || blank(event.lastName())) {
            log.warn("Ignoring unusable teacher event {}", event.eventId());
            return;
        }
        Optional<StaffMember> existing = staff.findByKeycloakId(event.keycloakId());
        if (existing.isEmpty()) {
            staff.save(new StaffMember(event.tenantId(), event.keycloakId(), event.email(),
                    event.firstName(), event.lastName()));
        } else if (existing.get().getTenantId().equals(event.tenantId())) {
            existing.get().refresh(event.email(), event.firstName(), event.lastName());
        } else {
            // A login never moves between schools.
            log.warn("Ignoring teacher event {}: that login belongs to another school", event.eventId());
        }
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }
}