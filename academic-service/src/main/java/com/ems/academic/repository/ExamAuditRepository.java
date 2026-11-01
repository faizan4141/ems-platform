package com.ems.academic.repository;

import com.ems.academic.domain.ExamAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExamAuditRepository extends JpaRepository<ExamAudit, UUID> {
    List<ExamAudit> findTop200ByExamIdOrderByAtDesc(UUID examId);
}