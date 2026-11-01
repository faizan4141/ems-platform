package com.ems.academic.dto;

import com.ems.academic.domain.ExamStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ExamDtos {

    private ExamDtos() {}

    public record SubjectRequest(@NotBlank @Size(max = 100) String name, @Size(max = 20) String code) {}
    public record SubjectView(UUID id, String name, String code) {}

    public record BandRequest(
            @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal minPercent,
            @NotBlank @Size(max = 10) String grade,
            @Size(max = 50) String label) {}
    public record ScaleRequest(@NotEmpty @Size(max = 12) List<@Valid BandRequest> bands) {}
    public record BandView(BigDecimal minPercent, String grade, String label) {}

    public record ExamSubjectInput(
            @NotNull UUID subjectId,
            @NotNull @DecimalMin("0.01") @DecimalMax("1000") @Digits(integer = 4, fraction = 2) BigDecimal maxMarks,
            @NotNull @DecimalMin("0") @Digits(integer = 4, fraction = 2) BigDecimal passMarks) {}
    public record ExamRequest(
            @NotNull UUID classId,
            @NotBlank @Size(max = 100) String name,
            @NotEmpty @Size(max = 15) List<@Valid ExamSubjectInput> subjects) {}
    public record ExamSubjectView(UUID examSubjectId, UUID subjectId, String subjectName,
                                  BigDecimal maxMarks, BigDecimal passMarks) {}
    public record ExamView(UUID id, UUID classId, String className, String name, ExamStatus status,
                           Instant publishedAt, List<ExamSubjectView> subjects) {}

    /** absent may be omitted (treated as false). A null mark with absent=false clears the entry. */
    public record MarkEntry(@NotNull UUID studentId, @NotNull UUID examSubjectId, BigDecimal marks, Boolean absent) {}
    public record MarksRequest(@NotEmpty @Size(max = 2000) List<@Valid MarkEntry> entries) {}
    public record MarksSaveResponse(int received, int changed) {}

    public record GridCell(BigDecimal marks, boolean absent) {}
    public record GridRow(UUID studentId, String name, String admissionNo, Map<UUID, GridCell> cells) {}
    public record MarksGrid(ExamView exam, List<GridRow> rows) {}

    public record ReportLine(String subject, BigDecimal maxMarks, BigDecimal passMarks, BigDecimal marks,
                             boolean absent, BigDecimal percent, String grade, Boolean passed) {}
    public record ReportCard(UUID examId, String examName, ExamStatus status, Instant publishedAt, String schoolName,
                             String className, String sectionName, UUID studentId, String studentName,
                             String admissionNo, List<ReportLine> lines, BigDecimal totalMarks, BigDecimal totalMax,
                             BigDecimal percent, String grade, String result, boolean complete) {}

    public record ResultRow(UUID studentId, String name, String admissionNo, BigDecimal totalMarks,
                            BigDecimal totalMax, BigDecimal percent, String grade, String result, boolean complete) {}
    public record ClassResults(UUID examId, List<ResultRow> rows) {}

    public record ChildExam(UUID examId, String name, Instant publishedAt, BigDecimal percent, String grade, String result) {}
    public record ReopenRequest(@NotBlank @Size(max = 200) String reason) {}
    public record AuditView(String actor, String action, String detail, Instant at) {}
}