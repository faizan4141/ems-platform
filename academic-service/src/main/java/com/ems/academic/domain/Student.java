package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "admission_no", nullable = false, length = 30) private String admissionNo;
    @Column(name = "first_name", nullable = false, length = 100) private String firstName;
    @Column(name = "last_name", nullable = false, length = 100) private String lastName;
    @Column(name = "date_of_birth", nullable = false) private LocalDate dateOfBirth;
    @Enumerated(EnumType.STRING) @Column(length = 10) private Gender gender;
    @Column(name = "admission_date", nullable = false) private LocalDate admissionDate;
    @Column(name = "section_id") private UUID sectionId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private StudentStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Student() {}

    public Student(UUID tenantId, String admissionNo, String firstName, String lastName, LocalDate dateOfBirth,
                   Gender gender, LocalDate admissionDate, UUID sectionId) {
        this.tenantId = tenantId;
        this.admissionNo = admissionNo;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.admissionDate = admissionDate;
        this.sectionId = sectionId;
        this.status = StudentStatus.ACTIVE;
    }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); this.updatedAt = this.createdAt; }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public void update(String firstName, String lastName, LocalDate dateOfBirth, Gender gender,
                       UUID sectionId, StudentStatus status) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.sectionId = sectionId;
        this.status = status;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getAdmissionNo() { return admissionNo; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Gender getGender() { return gender; }
    public LocalDate getAdmissionDate() { return admissionDate; }
    public UUID getSectionId() { return sectionId; }
    public StudentStatus getStatus() { return status; }
}