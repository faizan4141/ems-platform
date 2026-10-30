package com.ems.academic.dto;

import com.ems.academic.domain.Notice;
import com.ems.academic.domain.NoticeAudience;

import java.time.Instant;
import java.util.UUID;

public record NoticeResponse(UUID id, String title, String body, NoticeAudience audience, Instant publishedAt) {

    public static NoticeResponse from(Notice n) {
        return new NoticeResponse(n.getId(), n.getTitle(), n.getBody(), n.getAudience(), n.getPublishedAt());
    }
}