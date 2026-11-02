package com.ems.academic.repository;

import com.ems.academic.domain.StaffMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffMemberRepository extends JpaRepository<StaffMember, UUID> {
    Optional<StaffMember> findByKeycloakId(UUID keycloakId);
    Optional<StaffMember> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<StaffMember> findByTenantIdAndKeycloakIdAndActiveTrue(UUID tenantId, UUID keycloakId);
    List<StaffMember> findByTenantIdOrderByLastNameAscFirstNameAsc(UUID tenantId);
}