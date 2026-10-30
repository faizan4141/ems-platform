package com.ems.academic.repository;

import com.ems.academic.domain.Notice;
import com.ems.academic.domain.NoticeAudience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    List<Notice> findTop50ByTenantIdAndArchivedFalseAndAudienceInOrderByPublishedAtDesc(
            UUID tenantId, Collection<NoticeAudience> audiences);

    Optional<Notice> findByIdAndTenantId(UUID id, UUID tenantId);
}