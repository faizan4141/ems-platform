package com.ems.academic.dto;

import com.ems.academic.domain.HomeworkKind;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class HomeworkDtos {

    private HomeworkDtos() {}

    public record HomeworkRequest(
            @NotNull UUID sectionId,
            @NotNull UUID subjectId,
            @NotNull HomeworkKind kind,
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 4000) String description,
            LocalDate dueDate,
            @Size(max = 10) List<@Size(max = 500) String> links) {}

    public record HomeworkUpdate(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 4000) String description,
            LocalDate dueDate,
            @Size(max = 10) List<@Size(max = 500) String> links) {}

    public record HomeworkView(UUID id, UUID sectionId, String className, String sectionName, UUID subjectId,
                               String subjectName, HomeworkKind kind, String title, String description, LocalDate dueDate,
                               List<String> links, String authorName, Instant createdAt, boolean canEdit) {}

    public record ChildHomeworkItem(UUID id, String subjectName, HomeworkKind kind, String title, String description,
                                    LocalDate dueDate, List<String> links, String authorName, Instant createdAt) {}

    /** today is the school's date, so the screen can say "due today" without guessing the time zone. */
    public record ChildHomework(LocalDate today, List<ChildHomeworkItem> items) {}
}