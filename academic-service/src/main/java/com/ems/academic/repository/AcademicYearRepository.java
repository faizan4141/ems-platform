package com.ems.academic.repository;

import com.ems.academic.domain.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    List<AcademicYear> findByTenantIdOrderByStartDateDesc(UUID tenantId);
    Optional<AcademicYear> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<AcademicYear> findByTenantIdAndCurrentYearTrue(UUID tenantId);
    boolean existsByTenantIdAndName(UUID tenantId, String name);
    boolean existsByTenantIdAndCurrentYearTrue(UUID tenantId);
    boolean existsByTenantId(UUID tenantId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AcademicYear a set a.currentYear = false where a.tenantId = :tenantId and a.currentYear = true")
    int clearCurrent(@Param("tenantId") UUID tenantId);
}