package com.ems.academic.repository;

import com.ems.academic.domain.ExamSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExamSubjectRepository extends JpaRepository<ExamSubject, UUID> {
    List<ExamSubject> findByExamId(UUID examId);
    boolean existsBySubjectId(UUID subjectId);
}