package com.ems.academic.service;

import com.ems.academic.domain.*;
import com.ems.academic.dto.StaffDtos.*;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.*;
import com.ems.academic.security.Caller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StaffService {

    private static final Comparator<AssignmentView> ORDER = Comparator.comparing(AssignmentView::className)
            .thenComparing(AssignmentView::sectionName).thenComparing(a -> !a.classTeacher());

    private final StaffMemberRepository staff;
    private final TeachingAssignmentRepository assignments;
    private final SectionRepository sections;
    private final ClassRepository classes;
    private final SubjectRepository subjects;
    private final AcademicYearRepository years;
    private final TenantGuard guard;
    private final Clock clock;

    public StaffService(StaffMemberRepository staff, TeachingAssignmentRepository assignments, SectionRepository sections,
                        ClassRepository classes, SubjectRepository subjects, AcademicYearRepository years,
                        TenantGuard guard, Clock clock) {
        this.staff = staff;
        this.assignments = assignments;
        this.sections = sections;
        this.classes = classes;
        this.subjects = subjects;
        this.years = years;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<StaffView> list(UUID tenantId) {
        guard.requireActive(tenantId);
        List<StaffMember> members = staff.findByTenantIdOrderByLastNameAscFirstNameAsc(tenantId);
        List<TeachingAssignment> all = members.isEmpty() ? List.of()
                : assignments.findByStaffIdIn(members.stream().map(StaffMember::getId).toList());
        Map<UUID, AssignmentView> views = viewsById(all);
        Map<UUID, List<TeachingAssignment>> byStaff = all.stream().collect(Collectors.groupingBy(TeachingAssignment::getStaffId));
        return members.stream().map(m -> new StaffView(m.getId(), m.getFirstName() + " " + m.getLastName(),
                m.getEmailLower(), m.getDesignation(), m.isActive(),
                byStaff.getOrDefault(m.getId(), List.of()).stream().map(a -> views.get(a.getId())).sorted(ORDER).toList())).toList();
    }

    @Transactional
    public StaffView update(UUID tenantId, UUID staffId, StaffUpdate req) {
        guard.requireActive(tenantId);
        StaffMember member = requireStaff(tenantId, staffId);
        String designation = req.designation() == null || req.designation().isBlank() ? null : req.designation().trim();
        member.update(designation, req.active());
        return list(tenantId).stream().filter(s -> s.id().equals(staffId)).findFirst().orElseThrow();
    }

    @Transactional
    public AssignmentView assign(UUID tenantId, AssignmentRequest req) {
        guard.requireActive(tenantId);
        StaffMember member = requireStaff(tenantId, req.staffId());
        if (!member.isActive()) {
            throw new InvalidRequestException("This staff member is not active");
        }
        Section section = sections.findByIdAndTenantId(req.sectionId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        boolean classTeacher = Boolean.TRUE.equals(req.classTeacher());

        TeachingAssignment saved;
        if (classTeacher) {
            if (req.subjectId() != null) {
                throw new InvalidRequestException("A class teacher assignment does not have a subject");
            }
            Optional<TeachingAssignment> current = assignments.findBySectionIdAndClassTeacherTrue(section.getId());
            if (current.isPresent()) {
                String who = staff.findById(current.get().getStaffId())
                        .map(s -> s.getFirstName() + " " + s.getLastName()).orElse("another teacher");
                throw new DuplicateResourceException("This section already has a class teacher: " + who + ". Remove them first.");
            }
            saved = assignments.save(new TeachingAssignment(tenantId, section.getId(), member.getId(), null, true, clock.instant()));
        } else {
            if (req.subjectId() == null) {
                throw new InvalidRequestException("Choose the subject this teacher will teach");
            }
            subjects.findByIdAndTenantId(req.subjectId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
            if (assignments.existsBySectionIdAndStaffIdAndSubjectId(section.getId(), member.getId(), req.subjectId())) {
                throw new DuplicateResourceException("This teacher already teaches that subject in this section");
            }
            saved = assignments.save(new TeachingAssignment(tenantId, section.getId(), member.getId(), req.subjectId(), false, clock.instant()));
        }
        return viewsById(List.of(saved)).get(saved.getId());
    }

    @Transactional
    public void unassign(UUID tenantId, UUID assignmentId) {
        guard.requireActive(tenantId);
        assignments.delete(assignments.findByIdAndTenantId(assignmentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found")));
    }

    /** The sections the caller works with in the current academic year (empty for admins, who may use everything). */
    @Transactional(readOnly = true)
    public MyTeaching myTeaching(Caller caller) {
        if (caller.admin()) {
            return new MyTeaching(true, List.of());
        }
        UUID tenantId = caller.tenantId();
        guard.requireActive(tenantId);
        UUID login = parse(caller.subject());
        if (login == null) {
            return new MyTeaching(false, List.of());
        }
        Optional<StaffMember> member = staff.findByTenantIdAndKeycloakIdAndActiveTrue(tenantId, login);
        if (member.isEmpty()) {
            return new MyTeaching(false, List.of());
        }
        List<TeachingAssignment> mine = assignments.findByStaffId(member.get().getId());
        Map<UUID, Section> secs = sections.findAllById(mine.stream().map(TeachingAssignment::getSectionId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Section::getId, s -> s));
        Map<UUID, SchoolClass> cls = classes.findAllById(secs.values().stream().map(Section::getClassId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(SchoolClass::getId, c -> c));
        UUID currentYear = years.findByTenantIdAndCurrentYearTrue(tenantId).map(AcademicYear::getId).orElse(null);
        Map<UUID, String> subjectNames = subjects.findAllById(mine.stream().map(TeachingAssignment::getSubjectId)
                        .filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Subject::getId, Subject::getName));

        List<SectionAccess> result = new ArrayList<>();
        for (Map.Entry<UUID, List<TeachingAssignment>> entry : mine.stream()
                .collect(Collectors.groupingBy(TeachingAssignment::getSectionId)).entrySet()) {
            Section s = secs.get(entry.getKey());
            SchoolClass c = s == null ? null : cls.get(s.getClassId());
            if (s == null || c == null || (currentYear != null && !currentYear.equals(c.getAcademicYearId()))) continue;
            List<SubjectRef> refs = entry.getValue().stream().map(TeachingAssignment::getSubjectId).filter(Objects::nonNull)
                    .distinct().map(id -> new SubjectRef(id, subjectNames.getOrDefault(id, "?")))
                    .sorted(Comparator.comparing(SubjectRef::name)).toList();
            result.add(new SectionAccess(s.getId(), c.getId(), c.getName(), s.getName(),
                    entry.getValue().stream().anyMatch(TeachingAssignment::isClassTeacher), refs));
        }
        result.sort(Comparator.comparing(SectionAccess::className).thenComparing(SectionAccess::sectionName));
        return new MyTeaching(false, result);
    }

    private StaffMember requireStaff(UUID tenantId, UUID staffId) {
        return staff.findByIdAndTenantId(staffId, tenantId).orElseThrow(() -> new ResourceNotFoundException("Staff member not found"));
    }

    private Map<UUID, AssignmentView> viewsById(List<TeachingAssignment> list) {
        if (list.isEmpty()) return Map.of();
        Map<UUID, Section> secs = sections.findAllById(list.stream().map(TeachingAssignment::getSectionId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Section::getId, s -> s));
        Map<UUID, SchoolClass> cls = classes.findAllById(secs.values().stream().map(Section::getClassId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(SchoolClass::getId, c -> c));
        Set<UUID> subjectIds = list.stream().map(TeachingAssignment::getSubjectId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, String> names = subjectIds.isEmpty() ? Map.of() : subjects.findAllById(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));

        Map<UUID, AssignmentView> result = new HashMap<>();
        for (TeachingAssignment a : list) {
            Section s = secs.get(a.getSectionId());
            SchoolClass c = s == null ? null : cls.get(s.getClassId());
            result.put(a.getId(), new AssignmentView(a.getId(), a.getSectionId(), c == null ? "" : c.getName(),
                    s == null ? "" : s.getName(), a.getSubjectId(),
                    a.getSubjectId() == null ? null : names.get(a.getSubjectId()), a.isClassTeacher()));
        }
        return result;
    }

    private static UUID parse(String subject) {
        try {
            return subject == null ? null : UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}