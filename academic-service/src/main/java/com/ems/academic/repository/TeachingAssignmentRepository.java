package com.ems.academic.repository;

import com.ems.academic.domain.TeachingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, UUID> {
    List<TeachingAssignment> findByStaffId(UUID staffId);
    List<TeachingAssignment> findByStaffIdIn(Collection<UUID> staffIds);
    Optional<TeachingAssignment> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<TeachingAssignment> findBySectionIdAndClassTeacherTrue(UUID sectionId);
    boolean existsBySectionIdAndStaffIdAndSubjectId(UUID sectionId, UUID staffId, UUID subjectId);
    boolean existsBySubjectId(UUID subjectId);
}