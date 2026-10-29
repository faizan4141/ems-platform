package com.ems.academic.service;

import com.ems.academic.domain.AcademicYear;
import com.ems.academic.domain.SchoolClass;
import com.ems.academic.domain.Section;
import com.ems.academic.dto.ClassRequest;
import com.ems.academic.dto.ClassResponse;
import com.ems.academic.dto.SectionRequest;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.AcademicYearRepository;
import com.ems.academic.repository.ClassRepository;
import com.ems.academic.repository.SectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClassService {

    private final ClassRepository classes;
    private final SectionRepository sections;
    private final AcademicYearRepository years;
    private final TenantGuard guard;

    public ClassService(ClassRepository classes, SectionRepository sections,
                        AcademicYearRepository years, TenantGuard guard) {
        this.classes = classes;
        this.sections = sections;
        this.years = years;
        this.guard = guard;
    }

    /** Lists classes of the given year, or of the current year when none is given. */
    @Transactional(readOnly = true)
    public List<ClassResponse> list(UUID tenantId, UUID yearId) {
        guard.requireActive(tenantId);
        UUID resolved = yearId != null
                ? requireYear(tenantId, yearId).getId()
                : years.findByTenantIdAndCurrentYearTrue(tenantId).map(AcademicYear::getId).orElse(null);
        if (resolved == null) {
            return List.of();
        }
        List<SchoolClass> found =
                classes.findByTenantIdAndAcademicYearIdOrderByGradeLevelAscNameAsc(tenantId, resolved);
        if (found.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<Section>> byClass = sections
                .findByClassIdIn(found.stream().map(SchoolClass::getId).toList()).stream()
                .collect(Collectors.groupingBy(Section::getClassId));
        return found.stream()
                .map(c -> ClassResponse.from(c, byClass.getOrDefault(c.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ClassResponse create(UUID tenantId, ClassRequest req) {
        guard.requireActive(tenantId);
        AcademicYear year = requireYear(tenantId, req.academicYearId());
        String name = req.name().trim();
        if (classes.existsByAcademicYearIdAndName(year.getId(), name)) {
            throw new DuplicateResourceException("Class already exists in this year: " + name);
        }
        List<String> sectionNames = normalize(req.sections());

        SchoolClass created = classes.save(new SchoolClass(tenantId, year.getId(), name, req.gradeLevel()));
        List<Section> saved = sectionNames.stream()
                .map(n -> sections.save(new Section(tenantId, created.getId(), n, null)))
                .toList();
        return ClassResponse.from(created, saved);
    }

    @Transactional
    public ClassResponse addSection(UUID tenantId, UUID classId, SectionRequest req) {
        guard.requireActive(tenantId);
        SchoolClass schoolClass = classes.findByIdAndTenantId(classId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found"));
        String name = req.name().trim();
        if (sections.existsByClassIdAndName(classId, name)) {
            throw new DuplicateResourceException("Section already exists: " + name);
        }
        sections.save(new Section(tenantId, classId, name, req.capacity()));
        return ClassResponse.from(schoolClass, sections.findByClassId(classId));
    }

    private AcademicYear requireYear(UUID tenantId, UUID yearId) {
        return years.findByIdAndTenantId(yearId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
    }

    private List<String> normalize(List<String> raw) {
        if (raw == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String s : raw) {
            String n = s.trim();
            if (!seen.add(n.toLowerCase())) {
                throw new InvalidRequestException("Duplicate section name: " + n);
            }
            result.add(n);
        }
        return result;
    }
}