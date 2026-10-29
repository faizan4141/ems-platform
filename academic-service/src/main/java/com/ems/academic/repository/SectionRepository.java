package com.ems.academic.repository;

import com.ems.academic.domain.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface SectionRepository extends JpaRepository<Section, UUID> {

    List<Section> findByClassIdIn(Collection<UUID> classIds);
    List<Section> findByClassId(UUID classId);
    boolean existsByClassIdAndName(UUID classId, String name);
    Optional<Section> findByIdAndTenantId(UUID id, UUID tenantId);
}