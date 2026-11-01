package com.ems.academic.repository;

import com.ems.academic.domain.FeeItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeeItemRepository extends JpaRepository<FeeItem, UUID> {
    List<FeeItem> findByTenantIdAndClassIdOrderByDueDateAscNameAsc(UUID tenantId, UUID classId);
    Optional<FeeItem> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByClassIdAndNameIgnoreCase(UUID classId, String name);
}