package com.ems.academic.repository;

import com.ems.academic.domain.ExamMark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ExamMarkRepository extends JpaRepository<ExamMark, UUID> {
    List<ExamMark> findByExamSubjectIdIn(Collection<UUID> examSubjectIds);
    List<ExamMark> findByExamSubjectIdInAndStudentId(Collection<UUID> examSubjectIds, UUID studentId);
    long countByExamSubjectIdInAndStudentIdIn(Collection<UUID> examSubjectIds, Collection<UUID> studentIds);
}