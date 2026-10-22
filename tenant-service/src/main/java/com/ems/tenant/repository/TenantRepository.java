package com.ems.tenant.repository;

import com.ems.tenant.domain.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    boolean existsByCode(String code);
}