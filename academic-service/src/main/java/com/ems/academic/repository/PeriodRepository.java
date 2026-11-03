package com.ems.academic.repository;

import com.ems.academic.domain.SchoolPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PeriodRepository extends JpaRepository<SchoolPeriod, UUID> {
    List<SchoolPeriod> findByTenantIdOrderByNumberAsc(UUID tenantId);
    Optional<SchoolPeriod> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndNumber(UUID tenantId, int number);
}