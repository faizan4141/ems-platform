package com.ems.academic.repository;

import com.ems.academic.domain.GuardianLogin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface GuardianLoginRepository extends JpaRepository<GuardianLogin, UUID> {
    List<GuardianLogin> findByTenantIdAndEmailLowerIn(UUID tenantId, Collection<String> emails);
}