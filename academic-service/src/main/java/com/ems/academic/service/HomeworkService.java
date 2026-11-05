package com.ems.academic.service;

import com.ems.academic.domain.*;
import com.ems.academic.dto.HomeworkDtos.*;
import com.ems.academic.exception.OperationNotAllowedException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.*;
import com.ems.academic.security.Caller;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HomeworkService {

    private static final int STAFF_LIMIT = 200;
    private static final int PARENT_LIMIT = 100;

    private final HomeworkRepository homework;
    private final SectionRepository sections;
    private final ClassRepository classes;
    private final SubjectRepository subjects;
    private final ParentService parents;
    private final AccessService access;
    private final TenantGuard guard;
    private final Clock clock;

    public HomeworkService(HomeworkRepository homework, SectionRepository sections, ClassRepository classes,
                           SubjectRepository subjects, ParentService parents, AccessService access,
                           TenantGuard guard, Clock clock) {
        this.homework = homework;
        this.sections = sections;
        this.classes = classes;
        this.subjects = subjects;
        this.parents = parents;
        this.access = access;
        this.guard = guard;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<HomeworkView> list(Caller caller, UUID sectionId, HomeworkKind kind) {
        UUID tenantId = caller.tenantId();
        guard.requireActive(tenantId);
        AccessService.Scope scope = access.scope(caller);
        Pageable page = PageRequest.of(0, STAFF_LIMIT);

        List<Homework> found;
        if (sectionId != null) {
            requireSection(tenantId, sectionId);
            if (!scope.canSeeSection(sectionId)) {
                throw new OperationNotAllowedException("You are not assigned to this section");
            }
            found = homework.findByTenantIdAndSectionIdAndArchivedFalseOrderByCreatedAtDesc(tenantId, sectionId, page);
        } else if (scope.admin()) {
            found = homework.findByTenantIdAndArchivedFalseOrderByCreatedAtDesc(tenantId, page);
        } else if (scope.sections().isEmpty()) {
            return List.of();
        } else {
            found = homework.findByTenantIdAndSectionIdInAndArchivedFalseOrderByCreatedAtDesc(tenantId, scope.sections(), page);
        }
        return views(caller, scope, found.stream().filter(h -> kind == null || h.getKind() == kind).toList());
    }

    @Transactional
    public HomeworkView create(Caller caller, HomeworkRequest req) {
        UUID tenantId = caller.tenantId();
        guard.requireActive(tenantId);
        Section section = requireSection(tenantId, req.sectionId());
        subjects.findByIdAndTenantId(req.subjectId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        AccessService.Scope scope = access.scope(caller);
        if (!scope.canTeach(section.getId(), req.subjectId())) {
            throw new OperationNotAllowedException("You can only post for the subjects you teach in your own sections");
        }
        LocalDate due = HomeworkRules.checkDue(req.kind(), req.dueDate(), LocalDate.now(clock), true);
        List<String> links = HomeworkRules.normalizeLinks(req.links());

        Homework saved = homework.save(new Homework(tenantId, section.getId(), req.subjectId(), req.kind(),
                req.title().trim(), req.description().trim(), due, HomeworkRules.joinLinks(links),
                caller.subject(), caller.name(), clock.instant()));
        return views(caller, scope, List.of(saved)).get(0);
    }

    @Transactional
    public HomeworkView update(Caller caller, UUID id, HomeworkUpdate req) {
        UUID tenantId = caller.tenantId();
        guard.requireActive(tenantId);
        AccessService.Scope scope = access.scope(caller);
        Homework h = requireEditable(caller, scope, id);

        // Only a changed date has to be in the future, so a typo in last week's homework can still be fixed.
        boolean dueChanged = !Objects.equals(h.getDueDate(), req.dueDate());
        LocalDate due = HomeworkRules.checkDue(h.getKind(), req.dueDate(), LocalDate.now(clock), dueChanged);
        List<String> links = HomeworkRules.normalizeLinks(req.links());
        h.update(req.title().trim(), req.description().trim(), due, HomeworkRules.joinLinks(links), clock.instant());
        return views(caller, scope, List.of(h)).get(0);
    }

    @Transactional
    public void archive(Caller caller, UUID id) {
        UUID tenantId = caller.tenantId();
        guard.requireActive(tenantId);
        AccessService.Scope scope = access.scope(caller);
        requireEditable(caller, scope, id).archive(clock.instant());
    }

    @Transactional(readOnly = true)
    public ChildHomework forChild(UUID tenantId, String subject, UUID studentId) {
        Student child = parents.requireChild(tenantId, subject, studentId);   // only the parent's own children
        LocalDate today = LocalDate.now(clock);
        if (child.getSectionId() == null) {
            return new ChildHomework(today, List.of());
        }
        List<Homework> found = homework.findByTenantIdAndSectionIdAndArchivedFalseOrderByCreatedAtDesc(
                tenantId, child.getSectionId(), PageRequest.of(0, PARENT_LIMIT));
        Map<UUID, String> names = subjectNames(found);
        return new ChildHomework(today, found.stream().map(h -> new ChildHomeworkItem(h.getId(),
                names.getOrDefault(h.getSubjectId(), "?"), h.getKind(), h.getTitle(), h.getDescription(),
                h.getDueDate(), HomeworkRules.splitLinks(h.getLinks()), h.getAuthorName(), h.getCreatedAt())).toList());
    }

    // ---- helpers ----

    /** Admins may change any post; a teacher only their own, and only while they still teach there. */
    private Homework requireEditable(Caller caller, AccessService.Scope scope, UUID id) {
        Homework h = homework.findByIdAndTenantIdAndArchivedFalse(id, caller.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        if (!scope.canSeeSection(h.getSectionId())) {
            throw new ResourceNotFoundException("Post not found");   // outside their sections, so it looks like it does not exist
        }
        if (!canEdit(caller, scope, h)) {
            throw new OperationNotAllowedException("You can only change your own posts");
        }
        return h;
    }

    private static boolean canEdit(Caller caller, AccessService.Scope scope, Homework h) {
        return caller.admin()
                || (h.getAuthorLogin().equals(caller.subject()) && scope.canTeach(h.getSectionId(), h.getSubjectId()));
    }

    private Section requireSection(UUID tenantId, UUID sectionId) {
        return sections.findByIdAndTenantId(sectionId, tenantId).orElseThrow(() -> new ResourceNotFoundException("Section not found"));
    }

    private Map<UUID, String> subjectNames(List<Homework> list) {
        return subjects.findAllById(list.stream().map(Homework::getSubjectId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));
    }

    private List<HomeworkView> views(Caller caller, AccessService.Scope scope, List<Homework> list) {
        if (list.isEmpty()) return List.of();
        Map<UUID, Section> sectionById = sections.findAllById(list.stream().map(Homework::getSectionId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Section::getId, s -> s));
        Map<UUID, SchoolClass> classById = classes.findAllById(sectionById.values().stream().map(Section::getClassId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(SchoolClass::getId, c -> c));
        Map<UUID, String> names = subjectNames(list);

        return list.stream().map(h -> {
            Section s = sectionById.get(h.getSectionId());
            SchoolClass c = s == null ? null : classById.get(s.getClassId());
            return new HomeworkView(h.getId(), h.getSectionId(), c == null ? "" : c.getName(), s == null ? "" : s.getName(),
                    h.getSubjectId(), names.getOrDefault(h.getSubjectId(), "?"), h.getKind(), h.getTitle(), h.getDescription(),
                    h.getDueDate(), HomeworkRules.splitLinks(h.getLinks()), h.getAuthorName(), h.getCreatedAt(),
                    canEdit(caller, scope, h));
        }).toList();
    }
}