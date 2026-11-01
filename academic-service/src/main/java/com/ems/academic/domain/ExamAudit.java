package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "exam_audit")
public class ExamAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "exam_id", nullable = false, updatable = false) private UUID examId;
    @Column(nullable = false, length = 100, updatable = false) private String actor;
    @Column(nullable = false, length = 20, updatable = false) private String action;
    @Column(length = 300, updatable = false) private String detail;
    @Column(nullable = false, updatable = false) private Instant at;

    protected ExamAudit() {}

    public ExamAudit(UUID tenantId, UUID examId, String actor, String action, String detail, Instant at) {
        this.tenantId = tenantId;
        this.examId = examId;
        this.actor = actor;
        this.action = action;
        this.detail = detail != null && detail.length() > 300 ? detail.substring(0, 300) : detail;
        this.at = at;
    }

    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getDetail() { return detail; }
    public Instant getAt() { return at; }
}