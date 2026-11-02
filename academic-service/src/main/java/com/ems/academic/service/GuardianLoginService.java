package com.ems.academic.service;

import com.ems.academic.domain.GuardianLogin;
import com.ems.academic.event.UserLoginEvent;
import com.ems.academic.repository.GuardianLoginRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuardianLoginService {

    private static final Logger log = LoggerFactory.getLogger(GuardianLoginService.class);
    private final GuardianLoginRepository logins;

    public GuardianLoginService(GuardianLoginRepository logins) { this.logins = logins; }

    /** Safe to repeat: the login id is the key, so a redelivered event just rewrites the same row. */
    @Transactional
    public void apply(UserLoginEvent event) {
        if (!UserLoginEvent.PARENT_LOGIN_CREATED.equals(event.eventType())) {
            return;   // other event types belong to other services
        }
        if (event.keycloakId() == null || event.tenantId() == null || event.email() == null || event.email().isBlank()) {
            log.warn("Ignoring unusable user event {}", event.eventId());
            return;
        }
        GuardianLogin login = logins.findById(event.keycloakId())
                .orElseGet(() -> new GuardianLogin(event.keycloakId(), event.tenantId(), event.email()));
        login.link(event.tenantId(), event.email());
        logins.save(login);
    }
}