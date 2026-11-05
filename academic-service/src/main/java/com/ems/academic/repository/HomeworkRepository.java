package com.ems.academic.repository;

import com.ems.academic.domain.Homework;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HomeworkRepository extends JpaRepository<Homework, UUID> {

    Optional<Homework> findByIdAndTenantIdAndArchivedFalse(UUID id, UUID tenantId);

    List<Homework> findByTenantIdAndArchivedFalseOrderByCreatedAtDesc(UUID tenantId, Pageable page);

    List<Homework> findByTenantIdAndSectionIdAndArchivedFalseOrderByCreatedAtDesc(UUID tenantId, UUID sectionId, Pageable page);

    List<Homework> findByTenantIdAndSectionIdInAndArchivedFalseOrderByCreatedAtDesc(UUID tenantId, Collection<UUID> sectionIds, Pageable page);

    boolean existsBySubjectId(UUID subjectId);
}