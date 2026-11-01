package com.ems.academic.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "exam_subjects")
public class ExamSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "exam_id", nullable = false, updatable = false) private UUID examId;
    @Column(name = "subject_id", nullable = false, updatable = false) private UUID subjectId;
    @Column(name = "max_marks", nullable = false, precision = 6, scale = 2, updatable = false) private BigDecimal maxMarks;
    @Column(name = "pass_marks", nullable = false, precision = 6, scale = 2, updatable = false) private BigDecimal passMarks;

    protected ExamSubject() {}

    public ExamSubject(UUID examId, UUID subjectId, BigDecimal maxMarks, BigDecimal passMarks) {
        this.examId = examId;
        this.subjectId = subjectId;
        this.maxMarks = maxMarks;
        this.passMarks = passMarks;
    }

    public UUID getId() { return id; }
    public UUID getExamId() { return examId; }
    public UUID getSubjectId() { return subjectId; }
    public BigDecimal getMaxMarks() { return maxMarks; }
    public BigDecimal getPassMarks() { return passMarks; }
}