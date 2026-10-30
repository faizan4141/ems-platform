package com.ems.academic.repository;

import com.ems.academic.domain.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    List<AttendanceRecord> findBySessionId(UUID sessionId);
    List<AttendanceRecord> findBySessionIdIn(Collection<UUID> sessionIds);
}