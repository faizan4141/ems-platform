package com.ems.academic.service;

import com.ems.academic.domain.*;
import com.ems.academic.dto.*;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentRepository students;
    private final GuardianRepository guardians;
    private final SectionRepository sections;
    private final ClassRepository classes;
    private final TenantGuard guard;
    private final GuardianLoginRepository logins;

    public StudentService(StudentRepository students, GuardianRepository guardians, SectionRepository sections,
                          ClassRepository classes, TenantGuard guard, GuardianLoginRepository logins) {
        this.students = students;
        this.guardians = guardians;
        this.sections = sections;
        this.classes = classes;
        this.guard = guard;
        this.logins = logins;
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentSummary> list(UUID tenantId, UUID sectionId, boolean unassigned,
                                             String search, int page, int size) {
        guard.requireActive(tenantId);
        Specification<Student> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
        if (sectionId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sectionId"), sectionId));
        } else if (unassigned) {
            spec = spec.and((root, query, cb) -> cb.isNull(root.get("sectionId")));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + escapeLike(search.trim().toLowerCase(Locale.ROOT)) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.<String>get("firstName")), pattern, '\\'),
                    cb.like(cb.lower(root.<String>get("lastName")), pattern, '\\'),
                    cb.like(cb.lower(root.<String>get("admissionNo")), pattern, '\\'),
                    cb.like(cb.lower(cb.concat(cb.concat(root.<String>get("firstName"), " "),
                            root.<String>get("lastName"))), pattern, '\\')));
        }
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by("lastName", "firstName"));
        Page<Student> result = students.findAll(spec, pageable);

        Map<UUID, String[]> placement = placements(result.getContent().stream()
                .map(Student::getSectionId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return PageResponse.of(result, s -> {
            String[] p = placementOf(placement, s.getSectionId());
            return StudentSummary.from(s, p == null ? null : p[0], p == null ? null : p[1]);
        });
    }

    @Transactional(readOnly = true)
    public StudentDetail get(UUID tenantId, UUID id) {
        guard.requireActive(tenantId);
        return detail(require(tenantId, id), null);
    }

    @Transactional
    public StudentDetail create(UUID tenantId, StudentRequest req) {
        guard.requireActive(tenantId);
        String admissionNo = StudentFields.requireText(req.admissionNo(), "Admission number", 30);
        if (students.existsByTenantIdAndAdmissionNoIgnoreCase(tenantId, admissionNo)) {
            throw new DuplicateResourceException("Admission number already exists: " + admissionNo);
        }
        LocalDate dob = StudentFields.checkDob(req.dateOfBirth(), LocalDate.now());
        UUID sectionId = resolveSection(tenantId, req.sectionId());
        List<GuardianRequest> guardianInput = req.guardians();
        validateGuardians(guardianInput);

        Student student = students.save(new Student(tenantId, admissionNo,
                StudentFields.requireText(req.firstName(), "First name", 100),
                StudentFields.requireText(req.lastName(), "Last name", 100),
                dob, req.gender(), req.admissionDate() != null ? req.admissionDate() : LocalDate.now(), sectionId));
        List<Guardian> saved = guardians.saveAll(toGuardians(tenantId, student.getId(), guardianInput));
        return detail(student, saved);
    }

    @Transactional
    public StudentDetail update(UUID tenantId, UUID id, StudentUpdateRequest req) {
        guard.requireActive(tenantId);
        Student student = require(tenantId, id);
        student.update(StudentFields.requireText(req.firstName(), "First name", 100),
                StudentFields.requireText(req.lastName(), "Last name", 100),
                StudentFields.checkDob(req.dateOfBirth(), LocalDate.now()), req.gender(),
                resolveSection(tenantId, req.sectionId()), req.status());
        return detail(student, null);
    }

    @Transactional
    public StudentDetail replaceGuardians(UUID tenantId, UUID id, List<GuardianRequest> input) {
        guard.requireActive(tenantId);
        Student student = require(tenantId, id);
        validateGuardians(input);
        List<Guardian> replacement = toGuardians(tenantId, id, input);   // validate everything first
        guardians.deleteAllForStudent(id);
        return detail(student, guardians.saveAll(replacement));
    }

    // ---- helpers ----

    private Student require(UUID tenantId, UUID id) {
        return students.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
    }

    private UUID resolveSection(UUID tenantId, UUID sectionId) {
        if (sectionId == null) return null;
        return sections.findByIdAndTenantId(sectionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found")).getId();
    }

    private void validateGuardians(List<GuardianRequest> input) {
        if (input == null || input.isEmpty()) {
            throw new InvalidRequestException("At least one guardian is required");
        }
        long primaries = input.stream().filter(g -> Boolean.TRUE.equals(g.primary())).count();
        if (primaries > 1) {
            throw new InvalidRequestException("Only one guardian can be the primary contact");
        }
    }

    private List<Guardian> toGuardians(UUID tenantId, UUID studentId, List<GuardianRequest> input) {
        boolean anyPrimary = input.stream().anyMatch(g -> Boolean.TRUE.equals(g.primary()));
        List<Guardian> result = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            GuardianRequest g = input.get(i);
            boolean primary = anyPrimary ? Boolean.TRUE.equals(g.primary()) : i == 0;   // first one by default
            result.add(new Guardian(tenantId, studentId, StudentFields.requireText(g.name(), "Guardian name", 200),
                    g.relationship(), StudentFields.normalizePhone(g.phone()),
                    StudentFields.cleanEmail(g.email()), primary));
        }
        return result;
    }

    private StudentDetail detail(Student s, List<Guardian> known) {
        List<Guardian> list = known != null ? known : guardians.findByStudentId(s.getId());
        String[] p = placementOf(
                placements(s.getSectionId() == null ? Set.of() : Set.of(s.getSectionId())), s.getSectionId());
        return StudentDetail.from(s, list, portalEmails(s.getTenantId(), list),
                p == null ? null : p[0], p == null ? null : p[1]);
    }

    private Set<String> portalEmails(UUID tenantId, List<Guardian> list) {
        Set<String> emails = list.stream().map(Guardian::getEmail).filter(Objects::nonNull)
                .map(e -> e.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        if (emails.isEmpty()) {
            return Set.of();
        }
        return logins.findByTenantIdAndEmailLowerIn(tenantId, emails).stream()
                .map(GuardianLogin::getEmailLower).collect(Collectors.toSet());
    }

    /** sectionId to [className, sectionName]. */
    Map<UUID, String[]> placements(Collection<UUID> sectionIds) {
        if (sectionIds.isEmpty()) return Map.of();
        List<Section> found = sections.findAllById(sectionIds);
        Map<UUID, SchoolClass> byId = classes.findAllById(
                        found.stream().map(Section::getClassId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(SchoolClass::getId, c -> c));
        Map<UUID, String[]> result = new HashMap<>();
        for (Section s : found) {
            SchoolClass c = byId.get(s.getClassId());
            result.put(s.getId(), new String[]{c == null ? null : c.getName(), s.getName()});
        }
        return result;
    }

    /** An immutable empty map throws on get(null), so students without a section need this guard. */
    static String[] placementOf(Map<UUID, String[]> placements, UUID sectionId) {
        return sectionId == null ? null : placements.get(sectionId);
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}