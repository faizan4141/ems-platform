package com.ems.academic.web;

import com.ems.academic.dto.*;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.security.Caller;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.StudentImportService;
import com.ems.academic.service.StudentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/students")
public class StudentController {

    private final StudentService service;
    private final StudentImportService importService;

    public StudentController(StudentService service, StudentImportService importService) {
        this.service = service;
        this.importService = importService;
    }

    @GetMapping
    public PageResponse<StudentSummary> list(@AuthenticationPrincipal Jwt jwt,
                                             @RequestParam(required = false) UUID sectionId,
                                             @RequestParam(defaultValue = "false") boolean unassigned,
                                             @RequestParam(required = false) String search,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size,
                                             Authentication authentication) {
        return service.list(Caller.of(jwt, authentication), sectionId, unassigned, search, page, size);
    }

    @GetMapping("/{id}")
    public StudentDetail get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, Authentication authentication) {
        return service.get(Caller.of(jwt, authentication), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentDetail create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StudentRequest request) {
        return service.create(TenantClaims.requireTenantId(jwt), request);
    }

    @PutMapping("/{id}")
    public StudentDetail update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody StudentUpdateRequest request) {
        return service.update(TenantClaims.requireTenantId(jwt), id, request);
    }

    @PutMapping("/{id}/guardians")
    public StudentDetail replaceGuardians(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                          @RequestBody @NotEmpty @Size(max = 5) List<@Valid GuardianRequest> guardians) {
        return service.replaceGuardians(TenantClaims.requireTenantId(jwt), id, guardians);
    }

    /** dryRun=true (the default) only checks the file; dryRun=false saves the valid rows. */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importCsv(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file,
                                  @RequestParam(defaultValue = "true") boolean dryRun) {
        try {
            return importService.importCsv(TenantClaims.requireTenantId(jwt), file.getBytes(), dryRun);
        } catch (IOException e) {
            throw new InvalidRequestException("The file could not be read");
        }
    }
}