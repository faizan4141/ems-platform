package com.ems.academic.service;

import com.ems.academic.domain.*;
import com.ems.academic.dto.*;
import com.ems.academic.event.AttendanceRecorded;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.OperationNotAllowedException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ems.academic.security.Caller;
import com.ems.academic.exception.OperationNotAllowedException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceSessionRepository sessions;
    private final AttendanceRecordRepository records;
    private final StudentRepository students;
    private final GuardianRepository guardians;
    private final SectionRepository sections;
    private final ClassRepository classes;
    private final SchoolProfileRepository profiles;
    private final TenantGuard guard;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final AccessService access;

    public AttendanceService(AttendanceSessionRepository sessions, AttendanceRecordRepository records,
                             StudentRepository students, GuardianRepository guardians, SectionRepository sections,
                             ClassRepository classes, SchoolProfileRepository profiles, TenantGuard guard,
                             ApplicationEventPublisher events, Clock clock, AccessService access) {
        this.sessions = sessions;
        this.records = records;
        this.students = students;
        this.guardians = guardians;
        this.sections = sections;
        this.classes = classes;
        this.profiles = profiles;
        this.guard = guard;
        this.events = events;
        this.clock = clock;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public RosterResponse roster(Caller caller, UUID sectionId, LocalDate date) {
        UUID tenantId = caller.tenantId();
        guard.requireActive(tenantId);
        LocalDate day = resolveDate(date);
        Section section = requireSection(tenantId, sectionId);
        requireAccess(caller, section);
        SchoolClass schoolClass = requireClass(section);
        List<Student> roster = activeStudents(tenantId, sectionId);

        Optional<AttendanceSession> session = sessions.findBySectionIdAndAttendanceDate(sectionId, day);
        Map<UUID, AttendanceStatus> saved = session
                .map(s -> records.findBySessionId(s.getId()).stream()
                        .collect(Collectors.toMap(AttendanceRecord::getStudentId, AttendanceRecord::getStatus)))
                .orElse(Map.of());

        List<RosterEntry> entries = roster.stream()
                .map(s -> new RosterEntry(s.getId(), s.getAdmissionNo(), s.getFirstName() + " " + s.getLastName(),
                        saved.getOrDefault(s.getId(), AttendanceStatus.PRESENT)))
                .toList();
        return new RosterResponse(sectionId, schoolClass.getName(), section.getName(), day, session.isPresent(), entries);
    }

    @Transactional
    public SaveResponse save(Caller caller, UUID sectionId, LocalDate date, AttendanceRequest req) {
        UUID tenantId = caller.tenantId();
        boolean admin = caller.admin();
        String actor = caller.subject();
        TenantSnapshot tenant = guard.requireActive(tenantId);
        LocalDate today = LocalDate.now(clock);
        LocalDate day = date == null ? today : date;
        if (day.isAfter(today)) {
            throw new InvalidRequestException("Attendance cannot be taken for a future date");
        }
        if (!day.equals(today) && !admin) {
            throw new OperationNotAllowedException("Only school admins can change attendance for past dates");
        }

        Section section = requireSection(tenantId, sectionId);
        requireAccess(caller, section);
        SchoolClass schoolClass = requireClass(section);
        List<Student> roster = activeStudents(tenantId, sectionId);
        if (roster.isEmpty()) {
            throw new InvalidRequestException("This section has no active students");
        }

        Set<UUID> rosterIds = roster.stream().map(Student::getId).collect(Collectors.toSet());
        Map<UUID, AttendanceStatus> requested = new HashMap<>();
        for (AttendanceMark mark : req.marks()) {
            if (!rosterIds.contains(mark.studentId())) {
                throw new InvalidRequestException("A student in the request is not in this section");
            }
            if (requested.put(mark.studentId(), mark.status()) != null) {
                throw new InvalidRequestException("A student appears twice in the request");
            }
        }

        AttendanceSession session = sessions.findBySectionIdAndAttendanceDate(sectionId, day)
                .orElseGet(() -> sessions.save(new AttendanceSession(tenantId, sectionId, day, actor)));
        session.touch(actor);
        Map<UUID, AttendanceRecord> existing = records.findBySessionId(session.getId()).stream()
                .collect(Collectors.toMap(AttendanceRecord::getStudentId, r -> r));

        int present = 0, absent = 0, late = 0;
        List<Student> changed = new ArrayList<>();
        Map<UUID, AttendanceStatus> changedStatus = new HashMap<>();
        for (Student s : roster) {
            AttendanceStatus now = requested.getOrDefault(s.getId(), AttendanceStatus.PRESENT);
            AttendanceRecord record = existing.get(s.getId());
            AttendanceStatus before = record == null ? AttendanceStatus.PRESENT : record.getStatus();
            if (record == null) {
                records.save(new AttendanceRecord(tenantId, session.getId(), s.getId(), now));
            } else {
                record.setStatus(now);
            }
            switch (now) {
                case PRESENT -> present++;
                case ABSENT -> absent++;
                case LATE -> late++;
            }
            // Notify only when a student BECAME absent or late, so re-saving never repeats a message.
            if (now != AttendanceStatus.PRESENT && now != before) {
                changed.add(s);
                changedStatus.put(s.getId(), now);
            }
        }

        List<AttendanceRecorded.Alert> alerts = buildAlerts(changed, changedStatus);
        if (!alerts.isEmpty()) {
            String schoolName = profiles.findById(tenantId).map(SchoolProfile::getDisplayName).orElse(tenant.getName());
            events.publishEvent(new AttendanceRecorded(tenantId, session.getId(), day, schoolName,
                    schoolClass.getName(), section.getName(), alerts));
        }
        return new SaveResponse(present, absent, late, alerts.size());
    }

    @Transactional(readOnly = true)
    public AttendanceSummary summary(UUID tenantId, LocalDate date) {
        guard.requireActive(tenantId);
        LocalDate day = resolveDate(date);
        List<AttendanceSession> taken = sessions.findByTenantIdAndAttendanceDate(tenantId, day);
        int present = 0, absent = 0, late = 0;
        if (!taken.isEmpty()) {
            for (AttendanceRecord r : records.findBySessionIdIn(taken.stream().map(AttendanceSession::getId).toList())) {
                switch (r.getStatus()) {
                    case PRESENT -> present++;
                    case ABSENT -> absent++;
                    case LATE -> late++;
                }
            }
        }
        return AttendanceSummary.of(day, students.countSectionsWithStudents(tenantId), taken.size(), present, absent, late);
    }

    // ---- helpers ----

    private LocalDate resolveDate(LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        LocalDate day = date == null ? today : date;
        if (day.isAfter(today)) {
            throw new InvalidRequestException("Attendance cannot be viewed for a future date");
        }
        return day;
    }

    private Section requireSection(UUID tenantId, UUID sectionId) {
        return sections.findByIdAndTenantId(sectionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
    }

    private SchoolClass requireClass(Section section) {
        return classes.findById(section.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
    }

    private List<Student> activeStudents(UUID tenantId, UUID sectionId) {
        return students.findByTenantIdAndSectionIdAndStatusOrderByLastNameAscFirstNameAsc(
                tenantId, sectionId, StudentStatus.ACTIVE);
    }

    private List<AttendanceRecorded.Alert> buildAlerts(List<Student> changed, Map<UUID, AttendanceStatus> status) {
        if (changed.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<Guardian>> byStudent = guardians
                .findByStudentIdIn(changed.stream().map(Student::getId).toList()).stream()
                .collect(Collectors.groupingBy(Guardian::getStudentId));
        List<AttendanceRecorded.Alert> alerts = new ArrayList<>();
        for (Student s : changed) {
            List<Guardian> list = byStudent.getOrDefault(s.getId(), List.of());
            Guardian contact = list.stream().filter(Guardian::isPrimary).findFirst()
                    .orElse(list.isEmpty() ? null : list.get(0));
            if (contact == null) {
                continue;   // no one to notify
            }
            alerts.add(new AttendanceRecorded.Alert(s.getId(), s.getFirstName() + " " + s.getLastName(),
                    status.get(s.getId()), contact.getName(), contact.getPhone(), contact.getEmail()));
        }
        return alerts;
    }

    private void requireAccess(Caller caller, Section section) {
        if (!access.scope(caller).canSeeSection(section.getId())) {
            throw new OperationNotAllowedException("You are not assigned to this section");
        }
    }
}