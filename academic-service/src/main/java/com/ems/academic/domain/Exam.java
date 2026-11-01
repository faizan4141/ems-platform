package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exams")
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "class_id", nullable = false, updatable = false) private UUID classId;
    @Column(nullable = false, length = 100) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private ExamStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "created_by", nullable = false, length = 100, updatable = false) private String createdBy;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "published_by", length = 100) private String publishedBy;

    protected Exam() {}

    public Exam(UUID tenantId, UUID classId, String name, String createdBy, Instant now) {
        this.tenantId = tenantId;
        this.classId = classId;
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = now;
        this.status = ExamStatus.DRAFT;
    }

    public void publish(String actor, Instant now) {
        this.status = ExamStatus.PUBLISHED;
        this.publishedAt = now;
        this.publishedBy = actor;
    }

    public void reopen() {
        this.status = ExamStatus.DRAFT;
        this.publishedAt = null;
        this.publishedBy = null;
    }

    public UUID getId() { return id; }
    public UUID getClassId() { return classId; }
    public String getName() { return name; }
    public ExamStatus getStatus() { return status; }
    public Instant getPublishedAt() { return publishedAt; }
}