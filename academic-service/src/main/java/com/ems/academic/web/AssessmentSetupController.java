package com.ems.academic.web;

import com.ems.academic.dto.ExamDtos.*;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.GradingService;
import com.ems.academic.service.SubjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AssessmentSetupController {

    private final SubjectService subjects;
    private final GradingService grading;

    public AssessmentSetupController(SubjectService subjects, GradingService grading) {
        this.subjects = subjects;
        this.grading = grading;
    }

    @GetMapping("/subjects")
    public List<SubjectView> listSubjects(@AuthenticationPrincipal Jwt jwt) {
        return subjects.list(TenantClaims.requireTenantId(jwt));
    }

    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectView createSubject(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SubjectRequest request) {
        return subjects.create(TenantClaims.requireTenantId(jwt), request);
    }

    @DeleteMapping("/subjects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        subjects.delete(TenantClaims.requireTenantId(jwt), id);
    }

    @GetMapping("/grading-scale")
    public List<BandView> scale(@AuthenticationPrincipal Jwt jwt) {
        return grading.scale(TenantClaims.requireTenantId(jwt));
    }

    @PutMapping("/grading-scale")
    public List<BandView> saveScale(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ScaleRequest request) {
        return grading.save(TenantClaims.requireTenantId(jwt), request);
    }
}