package com.ems.academic.service;

import com.ems.academic.domain.Subject;
import com.ems.academic.dto.ExamDtos.SubjectRequest;
import com.ems.academic.dto.ExamDtos.SubjectView;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.ExamSubjectRepository;
import com.ems.academic.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SubjectService {

    private final SubjectRepository subjects;
    private final ExamSubjectRepository examSubjects;
    private final TenantGuard guard;

    public SubjectService(SubjectRepository subjects, ExamSubjectRepository examSubjects, TenantGuard guard) {
        this.subjects = subjects;
        this.examSubjects = examSubjects;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public List<SubjectView> list(UUID tenantId) {
        guard.requireActive(tenantId);
        return subjects.findByTenantIdOrderByNameAsc(tenantId).stream()
                .map(s -> new SubjectView(s.getId(), s.getName(), s.getCode())).toList();
    }

    @Transactional
    public SubjectView create(UUID tenantId, SubjectRequest req) {
        guard.requireActive(tenantId);
        String name = req.name().trim();
        if (subjects.existsByTenantIdAndNameIgnoreCase(tenantId, name)) {
            throw new DuplicateResourceException("Subject already exists: " + name);
        }
        String code = req.code() == null || req.code().isBlank() ? null : req.code().trim();
        Subject saved = subjects.save(new Subject(tenantId, name, code));
        return new SubjectView(saved.getId(), saved.getName(), saved.getCode());
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        guard.requireActive(tenantId);
        Subject subject = subjects.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Subject not found"));
        if (examSubjects.existsBySubjectId(id)) {
            throw new DuplicateResourceException("This subject is used by an exam and cannot be deleted");
        }
        subjects.delete(subject);
    }
}