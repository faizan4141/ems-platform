package com.ems.academic.service;

import com.ems.academic.domain.Student;
import com.ems.academic.dto.OnboardingResponse;
import com.ems.academic.repository.AcademicYearRepository;
import com.ems.academic.repository.ClassRepository;
import com.ems.academic.repository.SchoolProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ems.academic.repository.StudentRepository;

import java.util.UUID;

@Service
public class OnboardingService {

    private final SchoolProfileRepository profiles;
    private final AcademicYearRepository years;
    private final ClassRepository classes;
    private final TenantGuard guard;
    private final StudentRepository students;

    public OnboardingService(SchoolProfileRepository profiles, AcademicYearRepository years,
                             ClassRepository classes, TenantGuard guard, StudentRepository students) {
        this.profiles = profiles;
        this.years = years;
        this.classes = classes;
        this.guard = guard;
        this.students = students;
    }

    @Transactional(readOnly = true)
    public OnboardingResponse status(UUID tenantId) {
        guard.requireActive(tenantId);
        return new OnboardingResponse(profiles.existsById(tenantId), years.existsByTenantId(tenantId),
                classes.existsByTenantId(tenantId), students.existsByTenantId(tenantId));
    }
}