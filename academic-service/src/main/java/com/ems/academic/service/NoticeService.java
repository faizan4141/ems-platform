package com.ems.academic.service;

import com.ems.academic.domain.Notice;
import com.ems.academic.domain.NoticeAudience;
import com.ems.academic.dto.NoticeRequest;
import com.ems.academic.dto.NoticeResponse;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.NoticeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class NoticeService {

    private final NoticeRepository notices;
    private final TenantGuard guard;
    private final Clock clock;

    public NoticeService(NoticeRepository notices, TenantGuard guard, Clock clock) {
        this.notices = notices;
        this.guard = guard;
        this.clock = clock;
    }

    /** Parents see notices for everyone or for parents; staff see all of them. */
    @Transactional(readOnly = true)
    public List<NoticeResponse> list(UUID tenantId, boolean parentView) {
        guard.requireActive(tenantId);
        List<NoticeAudience> audiences = parentView
                ? List.of(NoticeAudience.ALL, NoticeAudience.PARENTS)
                : List.of(NoticeAudience.values());
        return notices.findTop50ByTenantIdAndArchivedFalseAndAudienceInOrderByPublishedAtDesc(tenantId, audiences)
                .stream().map(NoticeResponse::from).toList();
    }

    @Transactional
    public NoticeResponse create(UUID tenantId, NoticeRequest req, String actor) {
        guard.requireActive(tenantId);
        return NoticeResponse.from(notices.save(new Notice(tenantId, req.title().trim(), req.body().trim(),
                req.audience(), actor, clock.instant())));
    }

    @Transactional
    public void archive(UUID tenantId, UUID id) {
        guard.requireActive(tenantId);
        notices.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notice not found"))
                .archive();
    }
}