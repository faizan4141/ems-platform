package com.ems.academic.repository;

import com.ems.academic.domain.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {
    Optional<AttendanceSession> findBySectionIdAndAttendanceDate(UUID sectionId, LocalDate date);
    List<AttendanceSession> findByTenantIdAndAttendanceDate(UUID tenantId, LocalDate date);
}