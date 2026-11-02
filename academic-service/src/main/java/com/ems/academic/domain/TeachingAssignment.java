package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teaching_assignments")
public class TeachingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "section_id", nullable = false, updatable = false) private UUID sectionId;
    @Column(name = "staff_id", nullable = false, updatable = false) private UUID staffId;
    @Column(name = "subject_id", updatable = false) private UUID subjectId;
    @Column(name = "class_teacher", nullable = false, updatable = false) private boolean classTeacher;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected TeachingAssignment() {}

    public TeachingAssignment(UUID tenantId, UUID sectionId, UUID staffId, UUID subjectId, boolean classTeacher, Instant now) {
        this.tenantId = tenantId;
        this.sectionId = sectionId;
        this.staffId = staffId;
        this.subjectId = subjectId;
        this.classTeacher = classTeacher;
        this.createdAt = now;
    }

    public UUID getId() { return id; }
    public UUID getSectionId() { return sectionId; }
    public UUID getStaffId() { return staffId; }
    public UUID getSubjectId() { return subjectId; }
    public boolean isClassTeacher() { return classTeacher; }
}