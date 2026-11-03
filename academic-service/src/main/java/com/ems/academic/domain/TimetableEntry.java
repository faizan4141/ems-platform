package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "timetable_entries")
public class TimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "section_id", nullable = false, updatable = false) private UUID sectionId;
    @Column(name = "day_of_week", nullable = false, updatable = false) private int dayOfWeek;
    @Column(name = "period_id", nullable = false, updatable = false) private UUID periodId;
    @Column(name = "subject_id", nullable = false) private UUID subjectId;
    @Column(name = "staff_id", nullable = false) private UUID staffId;
    @Column(length = 30) private String room;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected TimetableEntry() {}

    public TimetableEntry(UUID tenantId, UUID sectionId, int dayOfWeek, UUID periodId, UUID subjectId,
                          UUID staffId, String room, Instant now) {
        this.tenantId = tenantId;
        this.sectionId = sectionId;
        this.dayOfWeek = dayOfWeek;
        this.periodId = periodId;
        this.createdAt = now;
        assign(subjectId, staffId, room);
    }

    public void assign(UUID subjectId, UUID staffId, String room) {
        this.subjectId = subjectId;
        this.staffId = staffId;
        this.room = room;
    }

    public UUID getId() { return id; }
    public UUID getSectionId() { return sectionId; }
    public int getDayOfWeek() { return dayOfWeek; }
    public UUID getPeriodId() { return periodId; }
    public UUID getSubjectId() { return subjectId; }
    public UUID getStaffId() { return staffId; }
    public String getRoom() { return room; }
}