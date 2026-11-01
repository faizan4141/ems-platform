package com.ems.academic.service;

import com.ems.academic.domain.*;
import com.ems.academic.dto.ExamDtos.*;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.ExamAuditRepository;
import com.ems.academic.repository.ExamMarkRepository;
import com.ems.academic.repository.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MarksService {

    private final ExamRepository exams;
    private final ExamMarkRepository examMarks;
    private final ExamAuditRepository audits;
    private final ExamData examData;
    private final ExamService examService;
    private final GradingService grading;
    private final TenantGuard guard;
    private final Clock clock;

    public MarksService(ExamRepository exams, ExamMarkRepository examMarks, ExamAuditRepository audits,
                        ExamData examData, ExamService examService, GradingService grading, TenantGuard guard, Clock clock) {
        this.exams = exams;
        this.examMarks = examMarks;
        this.audits = audits;
        this.examData = examData;
        this.examService = examService;
        this.grading = grading;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MarksGrid grid(UUID tenantId, UUID examId, UUID sectionId) {
        guard.requireActive(tenantId);
        Exam exam = examService.require(tenantId, examId);
        ExamData.Loaded data = examData.load(examId);
        Map<UUID, Map<UUID, ExamMark>> byStudent = marksByStudent(data);
        List<GridRow> rows = examData.roster(tenantId, exam.getClassId(), sectionId).stream().map(s -> {
            Map<UUID, GridCell> cells = new LinkedHashMap<>();
            byStudent.getOrDefault(s.getId(), Map.of()).forEach((subjectId, m) -> cells.put(subjectId, new GridCell(m.getMarks(), m.isAbsent())));
            return new GridRow(s.getId(), s.getFirstName() + " " + s.getLastName(), s.getAdmissionNo(), cells);
        }).toList();
        return new MarksGrid(examService.view(exam), rows);
    }

    @Transactional(readOnly = true)
    public ClassResults results(UUID tenantId, UUID examId, UUID sectionId) {
        guard.requireActive(tenantId);
        Exam exam = examService.require(tenantId, examId);
        ExamData.Loaded data = examData.load(examId);
        List<GradeScale.Band> bands = grading.bandsFor(tenantId);
        Map<UUID, Map<UUID, ExamMark>> byStudent = marksByStudent(data);
        List<ResultRow> rows = examData.roster(tenantId, exam.getClassId(), sectionId).stream().map(s -> {
            Map<UUID, ReportCardCalculator.Entry> entries = new HashMap<>();
            byStudent.getOrDefault(s.getId(), Map.of()).forEach((subjectId, m) ->
                    entries.put(subjectId, new ReportCardCalculator.Entry(m.getMarks(), m.isAbsent())));
            ReportCardCalculator.Computation c = ReportCardCalculator.compute(data.inputs(), entries, bands);
            return new ResultRow(s.getId(), s.getFirstName() + " " + s.getLastName(), s.getAdmissionNo(),
                    c.totalMarks(), c.totalMax(), c.percent(), c.grade(), c.result(), c.complete());
        }).toList();
        return new ClassResults(examId, rows);
    }

    @Transactional
    public MarksSaveResponse save(UUID tenantId, UUID examId, MarksRequest req, String actor) {
        guard.requireActive(tenantId);
        Exam exam = exams.lockByIdAndTenantId(examId, tenantId).orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new InvalidRequestException("Results are published. Ask a school admin to reopen the exam before changing marks");
        }
        ExamData.Loaded data = examData.load(examId);
        Map<UUID, ExamSubject> subjectById = data.examSubjects().stream().collect(Collectors.toMap(ExamSubject::getId, s -> s));
        Map<UUID, Student> roster = examData.roster(tenantId, exam.getClassId(), null).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Map<String, ExamMark> existing = subjectById.isEmpty() ? Map.of()
                : examMarks.findByExamSubjectIdIn(subjectById.keySet()).stream()
                .collect(Collectors.toMap(m -> key(m.getExamSubjectId(), m.getStudentId()), m -> m));

        Set<String> seen = new HashSet<>();
        List<ExamAudit> trail = new ArrayList<>();
        Instant now = clock.instant();
        int changed = 0;

        for (MarkEntry entry : req.entries()) {
            ExamSubject subject = subjectById.get(entry.examSubjectId());
            Student student = roster.get(entry.studentId());
            if (subject == null) throw new InvalidRequestException("A subject in the request is not part of this exam");
            if (student == null) throw new InvalidRequestException("A student in the request is not in this class");
            String k = key(subject.getId(), student.getId());
            if (!seen.add(k)) throw new InvalidRequestException("The same student and subject appear twice in the request");

            boolean absent = Boolean.TRUE.equals(entry.absent());
            BigDecimal marks = MarkRules.normalize(entry.marks(), absent, subject.getMaxMarks());
            ExamMark current = existing.get(k);
            String label = student.getAdmissionNo() + ", " + data.names().get(subject.getSubjectId()) + ": ";

            if (marks == null && !absent) {                         // clear the entry
                if (current != null) {
                    examMarks.delete(current);
                    trail.add(new ExamAudit(tenantId, examId, actor, "MARK", label + text(current) + " → cleared", now));
                    changed++;
                }
            } else if (current == null) {
                examMarks.save(new ExamMark(tenantId, subject.getId(), student.getId(), marks, absent, now, actor));
                trail.add(new ExamAudit(tenantId, examId, actor, "MARK", label + "— → " + text(marks, absent), now));
                changed++;
            } else if (!current.same(marks, absent)) {
                trail.add(new ExamAudit(tenantId, examId, actor, "MARK", label + text(current) + " → " + text(marks, absent), now));
                current.set(marks, absent, now, actor);
                changed++;
            }
        }
        audits.saveAll(trail);
        return new MarksSaveResponse(req.entries().size(), changed);
    }

    private Map<UUID, Map<UUID, ExamMark>> marksByStudent(ExamData.Loaded data) {
        Set<UUID> ids = data.examSubjects().stream().map(ExamSubject::getId).collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return examMarks.findByExamSubjectIdIn(ids).stream().collect(
                Collectors.groupingBy(ExamMark::getStudentId, Collectors.toMap(ExamMark::getExamSubjectId, m -> m)));
    }

    private static String key(UUID examSubjectId, UUID studentId) { return examSubjectId + "|" + studentId; }

    private static String text(ExamMark m) { return text(m.getMarks(), m.isAbsent()); }

    private static String text(BigDecimal marks, boolean absent) {
        return absent ? "absent" : marks == null ? "—" : marks.toPlainString();
    }
}