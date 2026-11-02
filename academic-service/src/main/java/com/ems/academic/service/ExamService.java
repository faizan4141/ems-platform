package com.ems.academic.service;

import com.ems.academic.domain.*;
import com.ems.academic.dto.ExamDtos.*;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamService {

    private final ExamRepository exams;
    private final ExamSubjectRepository examSubjects;
    private final ExamMarkRepository examMarks;
    private final ExamAuditRepository audits;
    private final SubjectRepository subjects;
    private final ClassRepository classes;
    private final ExamData examData;
    private final TenantGuard guard;
    private final Clock clock;

    public ExamService(ExamRepository exams, ExamSubjectRepository examSubjects, ExamMarkRepository examMarks,
                       ExamAuditRepository audits, SubjectRepository subjects, ClassRepository classes,
                       ExamData examData, TenantGuard guard, Clock clock) {
        this.exams = exams;
        this.examSubjects = examSubjects;
        this.examMarks = examMarks;
        this.audits = audits;
        this.subjects = subjects;
        this.classes = classes;
        this.examData = examData;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ExamView> list(UUID tenantId, UUID classId) {
        guard.requireActive(tenantId);
        classes.findByIdAndTenantId(classId, tenantId).orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        return exams.findByTenantIdAndClassIdOrderByCreatedAtDesc(tenantId, classId).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public ExamView get(UUID tenantId, UUID examId) {
        guard.requireActive(tenantId);
        return view(require(tenantId, examId));
    }

    @Transactional
    public ExamView create(UUID tenantId, ExamRequest req, String actor) {
        guard.requireActive(tenantId);
        classes.findByIdAndTenantId(req.classId(), tenantId).orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        String name = req.name().trim();
        if (exams.existsByClassIdAndNameIgnoreCase(req.classId(), name)) {
            throw new DuplicateResourceException("This class already has an exam called " + name);
        }

        Set<UUID> subjectIds = req.subjects().stream().map(ExamSubjectInput::subjectId).collect(Collectors.toSet());
        if (subjectIds.size() != req.subjects().size()) {
            throw new InvalidRequestException("A subject appears twice in this exam");
        }
        List<Subject> found = subjects.findAllById(subjectIds);
        if (found.size() != subjectIds.size() || found.stream().anyMatch(s -> s == null)) {
            throw new ResourceNotFoundException("Subject not found");
        }
        for (ExamSubjectInput in : req.subjects()) {
            if (in.passMarks().compareTo(in.maxMarks()) > 0) {
                throw new InvalidRequestException("The pass mark cannot be more than the maximum mark");
            }
        }
        // Subjects belong to the school: confirm each one is this school's own.
        for (UUID id : subjectIds) {
            subjects.findByIdAndTenantId(id, tenantId).orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        }

        Instant now = clock.instant();
        Exam exam = exams.save(new Exam(tenantId, req.classId(), name, actor, now));
        examSubjects.saveAll(req.subjects().stream()
                .map(in -> new ExamSubject(exam.getId(), in.subjectId(), MarkRules.scale(in.maxMarks()), MarkRules.scale(in.passMarks())))
                .toList());
        audits.save(new ExamAudit(tenantId, exam.getId(), actor, "CREATE", "Exam created", now));
        return view(exam);
    }

    @Transactional
    public void delete(UUID tenantId, UUID examId, String actor) {
        guard.requireActive(tenantId);
        Exam exam = lock(tenantId, examId);
        if (exam.getStatus() == ExamStatus.PUBLISHED) {
            throw new InvalidRequestException("A published exam cannot be deleted. Reopen it first");
        }
        exams.delete(exam);
    }

    @Transactional
    public ExamView publish(UUID tenantId, UUID examId, String actor) {
        guard.requireActive(tenantId);
        Exam exam = lock(tenantId, examId);
        if (exam.getStatus() == ExamStatus.PUBLISHED) {
            throw new DuplicateResourceException("This exam is already published");
        }
        ExamData.Loaded data = examData.load(examId);
        List<UUID> subjectIds = data.examSubjects().stream().map(ExamSubject::getId).toList();
        List<UUID> studentIds = examData.roster(tenantId, exam.getClassId(), null, null).stream().map(Student::getId).toList();
        if (studentIds.isEmpty()) {
            throw new InvalidRequestException("There are no active students in this class");
        }
        long expected = (long) studentIds.size() * subjectIds.size();
        long actual = examMarks.countByExamSubjectIdInAndStudentIdIn(subjectIds, studentIds);
        if (actual < expected) {
            throw new InvalidRequestException((expected - actual) + " marks are still missing. Enter a mark, or AB for absent, "
                    + "for every student and subject before publishing");
        }
        Instant now = clock.instant();
        exam.publish(actor, now);
        audits.save(new ExamAudit(tenantId, examId, actor, "PUBLISH", "Results published", now));
        return view(exam);
    }

    @Transactional
    public ExamView reopen(UUID tenantId, UUID examId, String reason, String actor) {
        guard.requireActive(tenantId);
        Exam exam = lock(tenantId, examId);
        if (exam.getStatus() != ExamStatus.PUBLISHED) {
            throw new DuplicateResourceException("This exam is not published");
        }
        exam.reopen();
        audits.save(new ExamAudit(tenantId, examId, actor, "REOPEN", "Reopened: " + reason.trim(), clock.instant()));
        return view(exam);
    }

    @Transactional(readOnly = true)
    public List<AuditView> audit(UUID tenantId, UUID examId) {
        guard.requireActive(tenantId);
        require(tenantId, examId);
        return audits.findTop200ByExamIdOrderByAtDesc(examId).stream()
                .map(a -> new AuditView(a.getActor(), a.getAction(), a.getDetail(), a.getAt())).toList();
    }

    public Exam require(UUID tenantId, UUID examId) {
        return exams.findByIdAndTenantId(examId, tenantId).orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
    }

    private Exam lock(UUID tenantId, UUID examId) {
        return exams.lockByIdAndTenantId(examId, tenantId).orElseThrow(() -> new ResourceNotFoundException("Exam not found"));
    }

    public ExamView view(Exam e) {
        ExamData.Loaded data = examData.load(e.getId());
        String className = classes.findById(e.getClassId()).map(SchoolClass::getName).orElse("");
        List<ExamSubjectView> subs = data.examSubjects().stream()
                .map(es -> new ExamSubjectView(es.getId(), es.getSubjectId(), data.names().getOrDefault(es.getSubjectId(), "?"),
                        es.getMaxMarks(), es.getPassMarks()))
                .sorted(Comparator.comparing(ExamSubjectView::subjectName)).toList();
        return new ExamView(e.getId(), e.getClassId(), className, e.getName(), e.getStatus(), e.getPublishedAt(), subs);
    }
}