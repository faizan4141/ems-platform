package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "staff_members")
public class StaffMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "keycloak_id", nullable = false, updatable = false) private UUID keycloakId;
    @Column(name = "email_lower", nullable = false, length = 254) private String emailLower;
    @Column(name = "first_name", nullable = false, length = 100) private String firstName;
    @Column(name = "last_name", nullable = false, length = 100) private String lastName;
    @Column(length = 100) private String designation;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected StaffMember() {}

    public StaffMember(UUID tenantId, UUID keycloakId, String email, String firstName, String lastName) {
        this.tenantId = tenantId;
        this.keycloakId = keycloakId;
        this.active = true;
        refresh(email, firstName, lastName);
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); }

    /** Names and email follow the login account; designation and active status belong to the school. */
    public void refresh(String email, String firstName, String lastName) {
        this.emailLower = email.trim().toLowerCase(Locale.ROOT);
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
    }

    public void update(String designation, boolean active) {
        this.designation = designation;
        this.active = active;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getKeycloakId() { return keycloakId; }
    public String getEmailLower() { return emailLower; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getDesignation() { return designation; }
    public boolean isActive() { return active; }
}