package com.ems.academic.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "session_id", nullable = false, updatable = false) private UUID sessionId;
    @Column(name = "student_id", nullable = false, updatable = false) private UUID studentId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private AttendanceStatus status;

    protected AttendanceRecord() {}

    public AttendanceRecord(UUID tenantId, UUID sessionId, UUID studentId, AttendanceStatus status) {
        this.tenantId = tenantId;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.status = status;
    }

    public UUID getSessionId() { return sessionId; }
    public UUID getStudentId() { return studentId; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }
}