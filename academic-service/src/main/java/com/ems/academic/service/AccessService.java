package com.ems.academic.service;

import com.ems.academic.domain.TeachingAssignment;
import com.ems.academic.repository.StaffMemberRepository;
import com.ems.academic.repository.TeachingAssignmentRepository;
import com.ems.academic.security.Caller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AccessService {

    /** What one caller may touch. Admins may touch everything; teachers only what they are assigned to. */
    public record Scope(boolean admin, Set<UUID> sections, Set<UUID> classTeacherSections, Set<String> subjectPairs) {

        public static Scope everything() { return new Scope(true, Set.of(), Set.of(), Set.of()); }

        public static Scope none() { return new Scope(false, Set.of(), Set.of(), Set.of()); }

        public static String pair(UUID sectionId, UUID subjectId) { return sectionId + "|" + subjectId; }

        public static Scope of(List<TeachingAssignment> assignments) {
            Set<UUID> sections = new HashSet<>();
            Set<UUID> classTeacher = new HashSet<>();
            Set<String> pairs = new HashSet<>();
            for (TeachingAssignment a : assignments) {
                sections.add(a.getSectionId());
                if (a.isClassTeacher()) {
                    classTeacher.add(a.getSectionId());
                } else {
                    pairs.add(pair(a.getSectionId(), a.getSubjectId()));
                }
            }
            return new Scope(false, sections, classTeacher, pairs);
        }

        public boolean canSeeSection(UUID sectionId) {
            return admin || (sectionId != null && sections.contains(sectionId));
        }

        /** A class teacher may enter any subject for their own section; a subject teacher only their subject there. */
        public boolean canEditMarks(UUID sectionId, UUID subjectId) {
            if (admin) return true;
            if (sectionId == null || subjectId == null) return false;
            return classTeacherSections.contains(sectionId) || subjectPairs.contains(pair(sectionId, subjectId));
        }
    }

    private final StaffMemberRepository staff;
    private final TeachingAssignmentRepository assignments;

    public AccessService(StaffMemberRepository staff, TeachingAssignmentRepository assignments) {
        this.staff = staff;
        this.assignments = assignments;
    }

    @Transactional(readOnly = true)
    public Scope scope(Caller caller) {
        if (caller.admin()) {
            return Scope.everything();
        }
        UUID login = parse(caller.subject());
        if (login == null) {
            return Scope.none();
        }
        // No active staff record, no access: a teacher account the school has not set up sees nothing.
        return staff.findByTenantIdAndKeycloakIdAndActiveTrue(caller.tenantId(), login)
                .map(s -> Scope.of(assignments.findByStaffId(s.getId())))
                .orElse(Scope.none());
    }

    private static UUID parse(String subject) {
        try {
            return subject == null ? null : UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}