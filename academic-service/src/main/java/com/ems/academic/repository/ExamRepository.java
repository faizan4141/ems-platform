package com.ems.academic.repository;

import com.ems.academic.domain.Exam;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamRepository extends JpaRepository<Exam, UUID> {

    Optional<Exam> findByIdAndTenantId(UUID id, UUID tenantId);
    List<Exam> findByTenantIdAndClassIdOrderByCreatedAtDesc(UUID tenantId, UUID classId);
    boolean existsByClassIdAndNameIgnoreCase(UUID classId, String name);

    /** Serializes mark entry, publishing and reopening of one exam. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exam e where e.id = :id and e.tenantId = :tenantId")
    Optional<Exam> lockByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    /** Published exams in which the student has marks, newest first. Parents only ever see these. */
    @Query("select distinct e from Exam e, ExamSubject s, ExamMark m where m.examSubjectId = s.id "
            + "and s.examId = e.id and m.studentId = :studentId and e.tenantId = :tenantId "
            + "and e.status = com.ems.academic.domain.ExamStatus.PUBLISHED order by e.publishedAt desc")
    List<Exam> findPublishedForStudent(@Param("studentId") UUID studentId, @Param("tenantId") UUID tenantId);
}