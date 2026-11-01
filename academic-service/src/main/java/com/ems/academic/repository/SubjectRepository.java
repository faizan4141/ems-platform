package com.ems.academic.repository;

import com.ems.academic.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findByTenantIdOrderByNameAsc(UUID tenantId);
    Optional<Subject> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}