package com.ems.tenant.dto;

import com.ems.tenant.domain.TenantStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull TenantStatus status) {}