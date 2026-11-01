package com.ems.academic.repository;

import com.ems.academic.domain.GradeBand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GradeBandRepository extends JpaRepository<GradeBand, UUID> {

    List<GradeBand> findByTenantIdOrderByMinPercentDesc(UUID tenantId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from GradeBand b where b.tenantId = :tenantId")
    int deleteAllForTenant(@Param("tenantId") UUID tenantId);
}