package com.ems.academic.web;

import com.ems.academic.domain.HomeworkKind;
import com.ems.academic.dto.HomeworkDtos.*;
import com.ems.academic.security.Caller;
import com.ems.academic.service.HomeworkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/homework")
public class HomeworkController {

    private final HomeworkService service;

    public HomeworkController(HomeworkService service) { this.service = service; }

    @GetMapping
    public List<HomeworkView> list(@AuthenticationPrincipal Jwt jwt, Authentication authentication,
                                   @RequestParam(required = false) UUID sectionId,
                                   @RequestParam(required = false) HomeworkKind kind) {
        return service.list(Caller.of(jwt, authentication), sectionId, kind);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HomeworkView create(@AuthenticationPrincipal Jwt jwt, Authentication authentication,
                               @Valid @RequestBody HomeworkRequest request) {
        return service.create(Caller.of(jwt, authentication), request);
    }

    @PutMapping("/{id}")
    public HomeworkView update(@AuthenticationPrincipal Jwt jwt, Authentication authentication, @PathVariable UUID id,
                               @Valid @RequestBody HomeworkUpdate request) {
        return service.update(Caller.of(jwt, authentication), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@AuthenticationPrincipal Jwt jwt, Authentication authentication, @PathVariable UUID id) {
        service.archive(Caller.of(jwt, authentication), id);
    }
}