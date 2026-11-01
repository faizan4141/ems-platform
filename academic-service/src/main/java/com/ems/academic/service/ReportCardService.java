package com.ems.academic.service;

import com.ems.academic.domain.*;
import com.ems.academic.dto.ExamDtos.*;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportCardService {

    private final ExamRepository exams;
    private final ExamMarkRepository examMarks;
    private final StudentRepository students;
    private final SectionRepository sections;
    private final ClassRepository classes;
    private final SchoolProfileRepository profiles;
    private final ExamData examData;
    private final ExamService examService;
    private final GradingService grading;
    private final ParentService parents;
    private final TenantGuard guard;

    public ReportCardService(ExamRepository exams, ExamMarkRepository examMarks, StudentRepository students,
                             SectionRepository sections, ClassRepository classes, SchoolProfileRepository profiles,
                             ExamData examData, ExamService examService, GradingService grading, ParentService parents,
                             TenantGuard guard) {
        this.exams = exams;
        this.examMarks = examMarks;
        this.students = students;
        this.sections = sections;
        this.classes = classes;
        this.profiles = profiles;
        this.examData = examData;
        this.examService = examService;
        this.grading = grading;
        this.parents = parents;
        this.guard = guard;
    }

    /** Staff can open a report card at any time, including for a draft exam. */
    @Transactional(readOnly = true)
    public ReportCard forStaff(UUID tenantId, UUID examId, UUID studentId) {
        TenantSnapshot tenant = guard.requireActive(tenantId);
        Exam exam = examService.require(tenantId, examId);
        Student student = students.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        boolean inClass = student.getSectionId() != null && sections.findByIdAndTenantId(student.getSectionId(), tenantId)
                .map(s -> s.getClassId().equals(exam.getClassId())).orElse(false);
        if (!inClass) throw new ResourceNotFoundException("Student not found in this exam's class");
        return build(exam, student, tenant, grading.bandsFor(tenantId));
    }

    @Transactional(readOnly = true)
    public List<ChildExam> forParentList(UUID tenantId, String subject, UUID studentId) {
        parents.requireChild(tenantId, subject, studentId);
        List<GradeScale.Band> bands = grading.bandsFor(tenantId);
        return exams.findPublishedForStudent(studentId, tenantId).stream().map(e -> {
            ReportCardCalculator.Computation c = compute(e, studentId, bands);
            return new ChildExam(e.getId(), e.getName(), e.getPublishedAt(), c.percent(), c.grade(), c.result());
        }).toList();
    }

    @Transactional(readOnly = true)
    public ReportCard forParent(UUID tenantId, String subject, UUID studentId, UUID examId) {
        Student child = parents.requireChild(tenantId, subject, studentId);
        Exam exam = exams.findPublishedForStudent(studentId, tenantId).stream()
                .filter(e -> e.getId().equals(examId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Result not found"));
        TenantSnapshot tenant = guard.requireActive(tenantId);
        return build(exam, child, tenant, grading.bandsFor(tenantId));
    }

    private ReportCardCalculator.Computation compute(Exam exam, UUID studentId, List<GradeScale.Band> bands) {
        ExamData.Loaded data = examData.load(exam.getId());
        Map<UUID, ReportCardCalculator.Entry> entries = new HashMap<>();
        if (!data.examSubjects().isEmpty()) {
            examMarks.findByExamSubjectIdInAndStudentId(data.examSubjects().stream().map(ExamSubject::getId).toList(), studentId)
                    .forEach(m -> entries.put(m.getExamSubjectId(), new ReportCardCalculator.Entry(m.getMarks(), m.isAbsent())));
        }
        return ReportCardCalculator.compute(data.inputs(), entries, bands);
    }

    private ReportCard build(Exam exam, Student student, TenantSnapshot tenant, List<GradeScale.Band> bands) {
        ReportCardCalculator.Computation c = compute(exam, student.getId(), bands);
        String className = classes.findById(exam.getClassId()).map(SchoolClass::getName).orElse("");
        String sectionName = student.getSectionId() == null ? null
                : sections.findByIdAndTenantId(student.getSectionId(), tenant.getTenantId())
                .filter(s -> s.getClassId().equals(exam.getClassId())).map(Section::getName).orElse(null);
        String school = profiles.findById(tenant.getTenantId()).map(SchoolProfile::getDisplayName).orElse(tenant.getName());
        return new ReportCard(exam.getId(), exam.getName(), exam.getStatus(), exam.getPublishedAt(), school, className,
                sectionName, student.getId(), student.getFirstName() + " " + student.getLastName(), student.getAdmissionNo(),
                c.lines(), c.totalMarks(), c.totalMax(), c.percent(), c.grade(), c.result(), c.complete());
    }
}