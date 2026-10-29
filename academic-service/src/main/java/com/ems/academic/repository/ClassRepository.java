package com.ems.academic.repository;

import com.ems.academic.domain.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassRepository extends JpaRepository<SchoolClass, UUID> {

    List<SchoolClass> findByTenantIdAndAcademicYearIdOrderByGradeLevelAscNameAsc(UUID tenantId, UUID academicYearId);
    Optional<SchoolClass> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByAcademicYearIdAndName(UUID academicYearId, String name);
    boolean existsByTenantId(UUID tenantId);
}