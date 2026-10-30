package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, columnDefinition = "text") private String body;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private NoticeAudience audience;
    @Column(name = "published_at", nullable = false, updatable = false) private Instant publishedAt;
    @Column(name = "published_by", nullable = false, length = 100, updatable = false) private String publishedBy;
    @Column(nullable = false) private boolean archived;

    protected Notice() {}

    public Notice(UUID tenantId, String title, String body, NoticeAudience audience, String publishedBy, Instant now) {
        this.tenantId = tenantId;
        this.title = title;
        this.body = body;
        this.audience = audience;
        this.publishedBy = publishedBy;
        this.publishedAt = now;
    }

    public void archive() { this.archived = true; }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public NoticeAudience getAudience() { return audience; }
    public Instant getPublishedAt() { return publishedAt; }
    public boolean isArchived() { return archived; }
}