package com.ems.academic.web;

import com.ems.academic.dto.NoticeRequest;
import com.ems.academic.dto.NoticeResponse;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.NoticeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notices")
public class NoticeController {

    private final NoticeService service;

    public NoticeController(NoticeService service) { this.service = service; }

    @GetMapping
    public List<NoticeResponse> list(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        boolean parent = authentication.getAuthorities().stream().anyMatch(a -> "ROLE_PARENT".equals(a.getAuthority()));
        return service.list(TenantClaims.requireTenantId(jwt), parent);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoticeResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NoticeRequest request) {
        return service.create(TenantClaims.requireTenantId(jwt), request, jwt.getSubject());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.archive(TenantClaims.requireTenantId(jwt), id);
    }
}