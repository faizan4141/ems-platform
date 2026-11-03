package com.ems.academic.repository;

import com.ems.academic.domain.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, UUID> {
    List<TimetableEntry> findBySectionId(UUID sectionId);
    List<TimetableEntry> findByStaffId(UUID staffId);
    Optional<TimetableEntry> findBySectionIdAndDayOfWeekAndPeriodId(UUID sectionId, int day, UUID periodId);
    Optional<TimetableEntry> findByStaffIdAndDayOfWeekAndPeriodId(UUID staffId, int day, UUID periodId);
    Optional<TimetableEntry> findByTenantIdAndRoomIgnoreCaseAndDayOfWeekAndPeriodId(UUID tenantId, String room, int day, UUID periodId);
    boolean existsByPeriodId(UUID periodId);
    boolean existsBySubjectId(UUID subjectId);
    boolean existsBySectionIdAndStaffIdAndSubjectId(UUID sectionId, UUID staffId, UUID subjectId);
}