package com.ems.academic.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "guardians")
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "student_id", nullable = false, updatable = false) private UUID studentId;
    @Column(nullable = false, length = 200) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Relationship relationship;
    @Column(nullable = false, length = 20) private String phone;
    @Column(length = 254) private String email;
    @Column(name = "is_primary", nullable = false) private boolean primary;

    protected Guardian() {}

    public Guardian(UUID tenantId, UUID studentId, String name, Relationship relationship,
                    String phone, String email, boolean primary) {
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.name = name;
        this.relationship = relationship;
        this.phone = phone;
        this.email = email;
        this.primary = primary;
    }

    public UUID getId() { return id; }
    public UUID getStudentId() { return studentId; }
    public String getName() { return name; }
    public Relationship getRelationship() { return relationship; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public boolean isPrimary() { return primary; }
}