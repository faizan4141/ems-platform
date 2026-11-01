package com.ems.academic.web;

import com.ems.academic.dto.ExamDtos.*;
import com.ems.academic.security.Actors;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.ExamService;
import com.ems.academic.service.MarksService;
import com.ems.academic.service.ReportCardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams")
public class ExamController {

    private final ExamService exams;
    private final MarksService marks;
    private final ReportCardService reports;

    public ExamController(ExamService exams, MarksService marks, ReportCardService reports) {
        this.exams = exams;
        this.marks = marks;
        this.reports = reports;
    }

    @GetMapping
    public List<ExamView> list(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID classId) {
        return exams.list(TenantClaims.requireTenantId(jwt), classId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamView create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ExamRequest request) {
        return exams.create(TenantClaims.requireTenantId(jwt), request, Actors.name(jwt));
    }

    @GetMapping("/{id}")
    public ExamView get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return exams.get(TenantClaims.requireTenantId(jwt), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        exams.delete(TenantClaims.requireTenantId(jwt), id, Actors.name(jwt));
    }

    @GetMapping("/{id}/marks")
    public MarksGrid grid(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                          @RequestParam(required = false) UUID sectionId) {
        return marks.grid(TenantClaims.requireTenantId(jwt), id, sectionId);
    }

    @PutMapping("/{id}/marks")
    public MarksSaveResponse save(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                  @Valid @RequestBody MarksRequest request) {
        return marks.save(TenantClaims.requireTenantId(jwt), id, request, Actors.name(jwt));
    }

    @GetMapping("/{id}/results")
    public ClassResults results(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @RequestParam(required = false) UUID sectionId) {
        return marks.results(TenantClaims.requireTenantId(jwt), id, sectionId);
    }

    @PostMapping("/{id}/publish")
    public ExamView publish(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return exams.publish(TenantClaims.requireTenantId(jwt), id, Actors.name(jwt));
    }

    @PostMapping("/{id}/reopen")
    public ExamView reopen(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                           @Valid @RequestBody ReopenRequest request) {
        return exams.reopen(TenantClaims.requireTenantId(jwt), id, request.reason(), Actors.name(jwt));
    }

    @GetMapping("/{id}/audit")
    public List<AuditView> audit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return exams.audit(TenantClaims.requireTenantId(jwt), id);
    }

    @GetMapping("/{id}/report-card/{studentId}")
    public ReportCard reportCard(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID studentId) {
        return reports.forStaff(TenantClaims.requireTenantId(jwt), id, studentId);
    }
}