package com.ems.academic.service;

import com.ems.academic.domain.Notice;
import com.ems.academic.domain.NoticeAudience;
import com.ems.academic.dto.NoticeRequest;
import com.ems.academic.dto.NoticeResponse;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.NoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-30T05:00:00Z");

    @Mock NoticeRepository notices;
    @Mock TenantGuard guard;
    private NoticeService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new NoticeService(notices, guard, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void parentsAreOnlyOfferedNoticesForEveryoneOrParents() {
        service.list(tenantId, true);

        verify(notices).findTop50ByTenantIdAndArchivedFalseAndAudienceInOrderByPublishedAtDesc(
                tenantId, List.of(NoticeAudience.ALL, NoticeAudience.PARENTS));
    }

    @Test
    void staffSeeEveryAudience() {
        service.list(tenantId, false);

        verify(notices).findTop50ByTenantIdAndArchivedFalseAndAudienceInOrderByPublishedAtDesc(
                tenantId, List.of(NoticeAudience.values()));
    }

    @Test
    void createsATrimmedNoticeStampedWithTheClock() {
        when(notices.save(any(Notice.class))).thenAnswer(inv -> inv.getArgument(0));

        NoticeResponse res = service.create(tenantId, new NoticeRequest("  Sports day  ", " Friday ", NoticeAudience.ALL), "admin-1");

        assertThat(res.title()).isEqualTo("Sports day");
        assertThat(res.publishedAt()).isEqualTo(NOW);
    }

    @Test
    void cannotArchiveAnotherSchoolsNotice() {
        UUID id = UUID.randomUUID();
        when(notices.findByIdAndTenantId(id, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archive(tenantId, id)).isInstanceOf(ResourceNotFoundException.class);
    }
}