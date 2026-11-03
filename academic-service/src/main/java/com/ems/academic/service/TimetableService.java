package com.ems.academic.service;

import com.ems.academic.domain.*;
import com.ems.academic.dto.TimetableDtos.*;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.OperationNotAllowedException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.*;
import com.ems.academic.security.Caller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TimetableService {

    private final PeriodRepository periods;
    private final TimetableEntryRepository entries;
    private final SectionRepository sections;
    private final ClassRepository classes;
    private final SubjectRepository subjects;
    private final StaffMemberRepository staff;
    private final TeachingAssignmentRepository assignments;
    private final AcademicYearRepository years;
    private final ParentService parents;
    private final AccessService access;
    private final TenantGuard guard;
    private final Clock clock;

    public TimetableService(PeriodRepository periods, TimetableEntryRepository entries, SectionRepository sections,
                            ClassRepository classes, SubjectRepository subjects, StaffMemberRepository staff,
                            TeachingAssignmentRepository assignments, AcademicYearRepository years, ParentService parents,
                            AccessService access, TenantGuard guard, Clock clock) {
        this.periods = periods;
        this.entries = entries;
        this.sections = sections;
        this.classes = classes;
        this.subjects = subjects;
        this.staff = staff;
        this.assignments = assignments;
        this.years = years;
        this.parents = parents;
        this.access = access;
        this.guard = guard;
        this.clock = clock;
    }

    // ---- reading ----

    @Transactional(readOnly = true)
    public SectionTimetable sectionTimetable(Caller caller, UUID sectionId) {
        UUID tenantId = caller.tenantId();
        guard.requireActive(tenantId);
        Section section = requireSection(tenantId, sectionId);
        if (!access.scope(caller).canSeeSection(sectionId)) {
            throw new OperationNotAllowedException("You are not assigned to this section");
        }
        return build(tenantId, section);
    }

    @Transactional(readOnly = true)
    public SectionTimetable childTimetable(UUID tenantId, String subject, UUID studentId) {
        Student child = parents.requireChild(tenantId, subject, studentId);   // only the parent's own children
        if (child.getSectionId() == null) {
            return new SectionTimetable(null, null, null, periodViews(tenantId), List.of());
        }
        return build(tenantId, requireSection(tenantId, child.getSectionId()));
    }

    @Transactional(readOnly = true)
    public TeacherTimetable myTimetable(Caller caller) {
        return new TeacherTimetable(periodViews(caller.tenantId()), myLessons(caller));
    }

    @Transactional(readOnly = true)
    public TodayView myToday(Caller caller) {
        LocalDate today = LocalDate.now(clock);
        int day = today.getDayOfWeek().getValue();
        return new TodayView(today, day, myLessons(caller).stream().filter(l -> l.day() == day).toList());
    }

    // ---- changing (school admins) ----

    @Transactional
    public EntryView setSlot(UUID tenantId, UUID sectionId, int day, UUID periodId, SlotRequest req) {
        guard.requireActive(tenantId);
        if (day < 1 || day > 7) {
            throw new InvalidRequestException("The day must be 1 (Monday) to 7 (Sunday)");
        }
        Section section = requireSection(tenantId, sectionId);
        SchoolPeriod period = periods.findByIdAndTenantId(periodId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Period not found"));
        if (period.isBreakPeriod()) {
            throw new InvalidRequestException("Lessons cannot be scheduled in a break");
        }
        subjects.findByIdAndTenantId(req.subjectId(), tenantId).orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        StaffMember teacher = staff.findByIdAndTenantId(req.staffId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found"));
        String teacherName = teacher.getFirstName() + " " + teacher.getLastName();
        if (!teacher.isActive()) {
            throw new InvalidRequestException(teacherName + " is not active");
        }
        if (!assignments.existsBySectionIdAndStaffIdAndSubjectId(sectionId, teacher.getId(), req.subjectId())) {
            throw new InvalidRequestException(teacherName + " is not assigned to teach this subject in this section. "
                    + "Assign them on the Staff page first.");
        }
        String room = req.room() == null || req.room().isBlank() ? null : req.room().trim();

        entries.findByStaffIdAndDayOfWeekAndPeriodId(teacher.getId(), day, periodId)
                .filter(other -> !other.getSectionId().equals(sectionId))
                .ifPresent(other -> {
                    throw new DuplicateResourceException(teacherName + " is already teaching " + label(other.getSectionId())
                            + " in " + period.getName() + " on this day");
                });
        if (room != null) {
            entries.findByTenantIdAndRoomIgnoreCaseAndDayOfWeekAndPeriodId(tenantId, room, day, periodId)
                    .filter(other -> !other.getSectionId().equals(sectionId))
                    .ifPresent(other -> {
                        throw new DuplicateResourceException("Room " + room + " is already used by " + label(other.getSectionId())
                                + " in " + period.getName() + " on this day");
                    });
        }

        TimetableEntry entry = entries.findBySectionIdAndDayOfWeekAndPeriodId(sectionId, day, periodId).orElse(null);
        if (entry == null) {
            entry = entries.saveAndFlush(new TimetableEntry(tenantId, section.getId(), day, periodId, req.subjectId(),
                    teacher.getId(), room, clock.instant()));
        } else {
            entry.assign(req.subjectId(), teacher.getId(), room);
            entries.flush();   // let the database's clash guarantees speak now, not at commit
        }
        return entryViews(List.of(entry)).get(0);
    }

    @Transactional
    public void clearSlot(UUID tenantId, UUID sectionId, int day, UUID periodId) {
        guard.requireActive(tenantId);
        requireSection(tenantId, sectionId);
        entries.findBySectionIdAndDayOfWeekAndPeriodId(sectionId, day, periodId).ifPresent(entries::delete);
    }

    // ---- helpers ----

    private Section requireSection(UUID tenantId, UUID sectionId) {
        return sections.findByIdAndTenantId(sectionId, tenantId).orElseThrow(() -> new ResourceNotFoundException("Section not found"));
    }

    private String label(UUID sectionId) {
        return sections.findById(sectionId).map(s -> classes.findById(s.getClassId()).map(SchoolClass::getName).orElse("") + " · " + s.getName())
                .orElse("another class");
    }

    private SectionTimetable build(UUID tenantId, Section section) {
        String className = classes.findById(section.getClassId()).map(SchoolClass::getName).orElse("");
        return new SectionTimetable(section.getId(), className, section.getName(), periodViews(tenantId),
                entryViews(entries.findBySectionId(section.getId())));
    }

    private List<PeriodView> periodViews(UUID tenantId) {
        return periods.findByTenantIdOrderByNumberAsc(tenantId).stream().map(PeriodView::from).toList();
    }

    private List<EntryView> entryViews(List<TimetableEntry> list) {
        if (list.isEmpty()) return List.of();
        Map<UUID, String> subjectNames = subjects.findAllById(list.stream().map(TimetableEntry::getSubjectId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Subject::getId, Subject::getName));
        Map<UUID, String> teacherNames = staff.findAllById(list.stream().map(TimetableEntry::getStaffId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(StaffMember::getId, s -> s.getFirstName() + " " + s.getLastName()));
        return list.stream().map(e -> new EntryView(e.getId(), e.getDayOfWeek(), e.getPeriodId(), e.getSubjectId(),
                        subjectNames.getOrDefault(e.getSubjectId(), "?"), e.getStaffId(),
                        teacherNames.getOrDefault(e.getStaffId(), "?"), e.getRoom()))
                .sorted(Comparator.comparing(EntryView::day)).toList();
    }

    /** The caller's own lessons in the current academic year. Admins are not teachers here, so they get none. */
    private List<LessonView> myLessons(Caller caller) {
        if (caller.admin()) return List.of();
        UUID tenantId = caller.tenantId();
        guard.requireActive(tenantId);
        UUID login = parse(caller.subject());
        if (login == null) return List.of();
        Optional<StaffMember> me = staff.findByTenantIdAndKeycloakIdAndActiveTrue(tenantId, login);
        if (me.isEmpty()) return List.of();
        List<TimetableEntry> mine = entries.findByStaffId(me.get().getId());
        if (mine.isEmpty()) return List.of();

        Map<UUID, SchoolPeriod> periodById = periods.findByTenantIdOrderByNumberAsc(tenantId).stream()
                .collect(Collectors.toMap(SchoolPeriod::getId, p -> p));
        Map<UUID, Section> sectionById = sections.findAllById(mine.stream().map(TimetableEntry::getSectionId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Section::getId, s -> s));
        Map<UUID, SchoolClass> classById = classes.findAllById(sectionById.values().stream().map(Section::getClassId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(SchoolClass::getId, c -> c));
        Map<UUID, String> subjectNames = subjects.findAllById(mine.stream().map(TimetableEntry::getSubjectId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Subject::getId, Subject::getName));
        UUID currentYear = years.findByTenantIdAndCurrentYearTrue(tenantId).map(AcademicYear::getId).orElse(null);

        List<LessonView> lessons = new ArrayList<>();
        for (TimetableEntry e : mine) {
            SchoolPeriod p = periodById.get(e.getPeriodId());
            Section s = sectionById.get(e.getSectionId());
            SchoolClass c = s == null ? null : classById.get(s.getClassId());
            if (p == null || s == null || c == null || (currentYear != null && !currentYear.equals(c.getAcademicYearId()))) continue;
            lessons.add(new LessonView(e.getDayOfWeek(), p.getId(), p.getNumber(), p.getName(), p.getStartTime(), p.getEndTime(),
                    s.getId(), c.getName(), s.getName(), e.getSubjectId(), subjectNames.getOrDefault(e.getSubjectId(), "?"), e.getRoom()));
        }
        lessons.sort(Comparator.comparing(LessonView::day).thenComparing(LessonView::periodNumber));
        return lessons;
    }

    private static UUID parse(String subject) {
        try {
            return subject == null ? null : UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}