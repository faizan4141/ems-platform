package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "attendance_sessions")
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "section_id", nullable = false, updatable = false) private UUID sectionId;
    @Column(name = "attendance_date", nullable = false, updatable = false) private LocalDate attendanceDate;
    @Column(name = "taken_by", nullable = false, length = 100) private String takenBy;
    @Column(name = "taken_at", nullable = false, updatable = false) private Instant takenAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected AttendanceSession() {}

    public AttendanceSession(UUID tenantId, UUID sectionId, LocalDate attendanceDate, String takenBy) {
        this.tenantId = tenantId;
        this.sectionId = sectionId;
        this.attendanceDate = attendanceDate;
        this.takenBy = takenBy;
        this.takenAt = Instant.now();
        this.updatedAt = this.takenAt;
    }

    /** Records who saved last. */
    public void touch(String actor) {
        this.takenBy = actor;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
}