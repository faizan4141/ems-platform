package com.ems.academic.service;

import com.ems.academic.domain.AttendanceStatus;
import com.ems.academic.domain.Student;
import com.ems.academic.dto.AttendanceDay;
import com.ems.academic.dto.ChildSummary;
import com.ems.academic.dto.ChildToday;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.AttendanceRecordRepository;
import com.ems.academic.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ParentService {

    static final int RECENT_DAYS = 14;
    static final int WINDOW_DAYS = 30;

    private final StudentRepository students;
    private final AttendanceRecordRepository records;
    private final StudentService studentService;
    private final TenantGuard guard;
    private final Clock clock;

    public ParentService(StudentRepository students, AttendanceRecordRepository records,
                         StudentService studentService, TenantGuard guard, Clock clock) {
        this.students = students;
        this.records = records;
        this.studentService = studentService;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ChildSummary> children(UUID tenantId, String subject) {
        guard.requireActive(tenantId);
        List<Student> kids = linked(tenantId, subject);
        Map<UUID, String[]> placement = studentService.placements(
                kids.stream().map(Student::getSectionId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return kids.stream().map(k -> {
            String[] p = StudentService.placementOf(placement, k.getSectionId());
            return new ChildSummary(k.getId(), name(k), p == null ? null : p[0], p == null ? null : p[1]);
        }).toList();
    }

    @Transactional(readOnly = true)
    public ChildToday today(UUID tenantId, String subject, UUID studentId) {
        guard.requireActive(tenantId);
        // Only the parent's own children are visible; anything else looks like it does not exist.
        Student child = linked(tenantId, subject).stream().filter(s -> studentId.equals(s.getId())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Child not found"));

        LocalDate today = LocalDate.now(clock);
        List<AttendanceDay> days = records.findDays(child.getId(), today.minusDays(WINDOW_DAYS));
        AttendanceStatus todayStatus = days.stream().filter(d -> d.date().equals(today))
                .map(AttendanceDay::status).findFirst().orElse(null);

        UUID sectionId = child.getSectionId();
        String[] p = StudentService.placementOf(
                studentService.placements(sectionId == null ? Set.of() : Set.of(sectionId)), sectionId);
        return new ChildToday(child.getId(), name(child), p == null ? null : p[0], p == null ? null : p[1],
                todayStatus, percent(days), days.stream().limit(RECENT_DAYS).toList());
    }

    /** Late counts as attending. Null when nothing has been recorded. */
    static Integer percent(List<AttendanceDay> days) {
        if (days.isEmpty()) {
            return null;
        }
        long attending = days.stream().filter(d -> d.status() != AttendanceStatus.ABSENT).count();
        return (int) Math.round(100.0 * attending / days.size());
    }

    private List<Student> linked(UUID tenantId, String subject) {
        UUID keycloakId = parse(subject);
        return keycloakId == null ? List.of() : students.findChildren(keycloakId, tenantId);
    }

    private static UUID parse(String subject) {
        try {
            return subject == null ? null : UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String name(Student s) { return s.getFirstName() + " " + s.getLastName(); }
}