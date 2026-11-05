package com.ems.academic.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "homework")
public class Homework {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "section_id", nullable = false, updatable = false) private UUID sectionId;
    @Column(name = "subject_id", nullable = false, updatable = false) private UUID subjectId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10, updatable = false) private HomeworkKind kind;
    @Column(nullable = false, length = 200) private String title;
    @Column(nullable = false, columnDefinition = "text") private String description;
    @Column(name = "due_date") private LocalDate dueDate;
    @Column(columnDefinition = "text") private String links;
    @Column(name = "author_login", nullable = false, length = 100, updatable = false) private String authorLogin;
    @Column(name = "author_name", nullable = false, length = 100, updatable = false) private String authorName;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(nullable = false) private boolean archived;

    protected Homework() {}

    public Homework(UUID tenantId, UUID sectionId, UUID subjectId, HomeworkKind kind, String title, String description,
                    LocalDate dueDate, String links, String authorLogin, String authorName, Instant now) {
        this.tenantId = tenantId;
        this.sectionId = sectionId;
        this.subjectId = subjectId;
        this.kind = kind;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.links = links;
        this.authorLogin = authorLogin;
        this.authorName = authorName;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String description, LocalDate dueDate, String links, Instant now) {
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.links = links;
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        this.archived = true;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getSectionId() { return sectionId; }
    public UUID getSubjectId() { return subjectId; }
    public HomeworkKind getKind() { return kind; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDate getDueDate() { return dueDate; }
    public String getLinks() { return links; }
    public String getAuthorLogin() { return authorLogin; }
    public String getAuthorName() { return authorName; }
    public Instant getCreatedAt() { return createdAt; }
}