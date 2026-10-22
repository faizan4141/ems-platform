package com.ems.tenant.dto;

import com.ems.tenant.domain.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TenantResponse(
        UUID id, String code, String name, OrgType orgType, TenantStatus status,
        LocalDate contractStartDate, LocalDate contractEndDate, Instant createdAt) {

    public static TenantResponse from(Tenant t) {
        return new TenantResponse(t.getId(), t.getCode(), t.getName(), t.getOrgType(),
                t.getStatus(), t.getContractStartDate(), t.getContractEndDate(), t.getCreatedAt());
    }
}