package com.ems.academic.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exam_marks")
public class ExamMark {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "exam_subject_id", nullable = false, updatable = false) private UUID examSubjectId;
    @Column(name = "student_id", nullable = false, updatable = false) private UUID studentId;
    @Column(precision = 6, scale = 2) private BigDecimal marks;
    @Column(nullable = false) private boolean absent;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "updated_by", nullable = false, length = 100) private String updatedBy;

    protected ExamMark() {}

    public ExamMark(UUID tenantId, UUID examSubjectId, UUID studentId, BigDecimal marks, boolean absent,
                    Instant now, String actor) {
        this.tenantId = tenantId;
        this.examSubjectId = examSubjectId;
        this.studentId = studentId;
        set(marks, absent, now, actor);
    }

    public void set(BigDecimal marks, boolean absent, Instant now, String actor) {
        this.marks = absent ? null : marks;
        this.absent = absent;
        this.updatedAt = now;
        this.updatedBy = actor;
    }

    public boolean same(BigDecimal otherMarks, boolean otherAbsent) {
        if (absent != otherAbsent) return false;
        if (marks == null || otherMarks == null) return marks == null && otherMarks == null;
        return marks.compareTo(otherMarks) == 0;
    }

    public UUID getExamSubjectId() { return examSubjectId; }
    public UUID getStudentId() { return studentId; }
    public BigDecimal getMarks() { return marks; }
    public boolean isAbsent() { return absent; }
}