package com.ems.academic.repository;

import com.ems.academic.domain.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface GuardianRepository extends JpaRepository<Guardian, UUID> {

    List<Guardian> findByStudentId(UUID studentId);

    // Immediate bulk delete: a derived delete would run AFTER the inserts and trip the primary-guardian index.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Guardian g where g.studentId = :studentId")
    int deleteAllForStudent(@Param("studentId") UUID studentId);

    List<Guardian> findByStudentIdIn(Collection<UUID> studentIds);
}