package com.ems.tenant.service;

import com.ems.tenant.domain.*;
import com.ems.tenant.dto.*;
import com.ems.tenant.exception.*;
import com.ems.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock TenantRepository repository;
    @InjectMocks TenantService service;

    private CreateTenantRequest request(LocalDate start, LocalDate end) {
        return new CreateTenantRequest("dps-meerut", "Delhi Public School Meerut",
                OrgType.SCHOOL, start, end);
    }

    @Test
    void create_savesTenantAsActive() {
        when(repository.existsByCode("dps-meerut")).thenReturn(false);
        when(repository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

        TenantResponse res = service.create(
                request(LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31)));

        assertThat(res.code()).isEqualTo("dps-meerut");
        assertThat(res.status()).isEqualTo(TenantStatus.ACTIVE);
        verify(repository).save(any(Tenant.class));
    }

    @Test
    void create_rejectsDuplicateCode() {
        when(repository.existsByCode("dps-meerut")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                request(LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31))))
                .isInstanceOf(DuplicateTenantException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void create_rejectsEndDateBeforeStartDate() {
        assertThatThrownBy(() -> service.create(
                request(LocalDate.of(2027, 3, 31), LocalDate.of(2026, 4, 1))))
                .isInstanceOf(InvalidContractException.class);
    }
}