package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "guardian_logins")
public class GuardianLogin {

    @Id
    @Column(name = "keycloak_id")
    private UUID keycloakId;

    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "email_lower", nullable = false, length = 254) private String emailLower;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected GuardianLogin() {}

    public GuardianLogin(UUID keycloakId, UUID tenantId, String email) {
        this.keycloakId = keycloakId;
        link(tenantId, email);
    }

    public void link(UUID tenantId, String email) {
        this.tenantId = tenantId;
        this.emailLower = email.trim().toLowerCase(Locale.ROOT);
        this.updatedAt = Instant.now();
    }

    public String getEmailLower() { return emailLower; }
}