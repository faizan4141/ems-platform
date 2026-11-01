package com.ems.academic.service;

import com.ems.academic.domain.*;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.ExamSubjectRepository;
import com.ems.academic.repository.SectionRepository;
import com.ems.academic.repository.StudentRepository;
import com.ems.academic.repository.SubjectRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ExamData {

    public record Loaded(List<ExamSubject> examSubjects, Map<UUID, String> names,
                         List<ReportCardCalculator.SubjectInput> inputs) {}

    private final ExamSubjectRepository examSubjects;
    private final SubjectRepository subjects;
    private final SectionRepository sections;
    private final StudentRepository students;

    public ExamData(ExamSubjectRepository examSubjects, SubjectRepository subjects, SectionRepository sections,
                    StudentRepository students) {
        this.examSubjects = examSubjects;
        this.subjects = subjects;
        this.sections = sections;
        this.students = students;
    }

    /** The exam's subjects with their names, sorted by subject name. */
    public Loaded load(UUID examId) {
        List<ExamSubject> list = examSubjects.findByExamId(examId);
        Map<UUID, String> names = subjects.findAllById(list.stream().map(ExamSubject::getSubjectId).toList()).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));
        List<ReportCardCalculator.SubjectInput> inputs = list.stream()
                .map(x -> new ReportCardCalculator.SubjectInput(x.getId(), names.getOrDefault(x.getSubjectId(), "?"),
                        x.getMaxMarks(), x.getPassMarks()))
                .sorted(Comparator.comparing(ReportCardCalculator.SubjectInput::name)).toList();
        return new Loaded(list, names, inputs);
    }

    /** Active students currently in the class, optionally limited to one of its sections. */
    public List<Student> roster(UUID tenantId, UUID classId, UUID onlySection) {
        Set<UUID> sectionIds = sections.findByClassId(classId).stream().map(Section::getId).collect(Collectors.toSet());
        if (onlySection != null) {
            if (!sectionIds.contains(onlySection)) throw new ResourceNotFoundException("Section not found");
            sectionIds = Set.of(onlySection);
        }
        if (sectionIds.isEmpty()) return List.of();
        return students.findByTenantIdAndSectionIdInAndStatus(tenantId, sectionIds, StudentStatus.ACTIVE).stream()
                .sorted(Comparator.comparing(Student::getLastName).thenComparing(Student::getFirstName)).toList();
    }
}