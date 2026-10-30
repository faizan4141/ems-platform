package com.ems.academic.repository;

import com.ems.academic.domain.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ems.academic.dto.AttendanceDay;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    List<AttendanceRecord> findBySessionId(UUID sessionId);
    List<AttendanceRecord> findBySessionIdIn(Collection<UUID> sessionIds);

    @Query("select new com.ems.academic.dto.AttendanceDay(s.attendanceDate, r.status) "
            + "from AttendanceRecord r, AttendanceSession s "
            + "where r.sessionId = s.id and r.studentId = :studentId and s.attendanceDate >= :from "
            + "order by s.attendanceDate desc")
    List<AttendanceDay> findDays(@Param("studentId") UUID studentId, @Param("from") LocalDate from);
}