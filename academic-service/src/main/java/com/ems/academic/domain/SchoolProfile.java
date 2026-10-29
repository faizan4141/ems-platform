package com.ems.academic.domain;

import com.ems.academic.dto.ProfileRequest;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "school_profiles")
public class SchoolProfile {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "display_name", nullable = false, length = 200) private String displayName;
    @Column(length = 50) private String board;
    @Column(name = "affiliation_no", length = 50) private String affiliationNo;
    @Column(name = "address_line", length = 300) private String addressLine;
    @Column(length = 100) private String city;
    @Column(length = 100) private String state;
    @Column(name = "postal_code", length = 20) private String postalCode;
    @Column(length = 30) private String phone;
    @Column(length = 254) private String email;
    @Column(length = 300) private String website;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected SchoolProfile() {}

    public SchoolProfile(UUID tenantId) { this.tenantId = tenantId; }

    public void apply(ProfileRequest r) {
        this.displayName = r.displayName().trim();
        this.board = clean(r.board());
        this.affiliationNo = clean(r.affiliationNo());
        this.addressLine = clean(r.addressLine());
        this.city = clean(r.city());
        this.state = clean(r.state());
        this.postalCode = clean(r.postalCode());
        this.phone = clean(r.phone());
        this.email = clean(r.email());
        this.website = clean(r.website());
        this.updatedAt = Instant.now();
    }

    private static String clean(String v) { return v == null || v.isBlank() ? null : v.trim(); }

    public UUID getTenantId() { return tenantId; }
    public String getDisplayName() { return displayName; }
    public String getBoard() { return board; }
    public String getAffiliationNo() { return affiliationNo; }
    public String getAddressLine() { return addressLine; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getPostalCode() { return postalCode; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getWebsite() { return website; }
}