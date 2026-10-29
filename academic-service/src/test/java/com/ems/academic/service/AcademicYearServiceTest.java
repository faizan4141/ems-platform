package com.ems.academic.service;

import com.ems.academic.domain.AcademicYear;
import com.ems.academic.dto.YearRequest;
import com.ems.academic.dto.YearResponse;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.repository.AcademicYearRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicYearServiceTest {

    @Mock AcademicYearRepository years;
    @Mock TenantGuard guard;
    @InjectMocks AcademicYearService service;

    private final UUID tenantId = UUID.randomUUID();

    private YearRequest request(String name, String start, String end, Boolean makeCurrent) {
        return new YearRequest(name, LocalDate.parse(start), LocalDate.parse(end), makeCurrent);
    }

    @Test
    void firstYearBecomesCurrent() {
        when(years.existsByTenantIdAndName(tenantId, "2026-27")).thenReturn(false);
        when(years.existsByTenantIdAndCurrentYearTrue(tenantId)).thenReturn(false);
        when(years.save(any(AcademicYear.class))).thenAnswer(inv -> inv.getArgument(0));

        YearResponse res = service.create(tenantId, request("2026-27", "2026-04-01", "2027-03-31", null));

        assertThat(res.current()).isTrue();
        verify(years).clearCurrent(tenantId);
    }

    @Test
    void laterYearIsNotCurrentUnlessRequested() {
        when(years.existsByTenantIdAndName(tenantId, "2027-28")).thenReturn(false);
        when(years.existsByTenantIdAndCurrentYearTrue(tenantId)).thenReturn(true);
        when(years.save(any(AcademicYear.class))).thenAnswer(inv -> inv.getArgument(0));

        YearResponse res = service.create(tenantId, request("2027-28", "2027-04-01", "2028-03-31", false));

        assertThat(res.current()).isFalse();
        verify(years, never()).clearCurrent(any());
    }

    @Test
    void rejectsEndDateNotAfterStartDate() {
        assertThatThrownBy(() -> service.create(tenantId, request("bad", "2027-03-31", "2026-04-01", null)))
                .isInstanceOf(InvalidRequestException.class);
        verify(years, never()).save(any());
    }

    @Test
    void rejectsDuplicateName() {
        when(years.existsByTenantIdAndName(tenantId, "2026-27")).thenReturn(true);

        assertThatThrownBy(() -> service.create(tenantId, request("2026-27", "2026-04-01", "2027-03-31", null)))
                .isInstanceOf(DuplicateResourceException.class);
    }
}