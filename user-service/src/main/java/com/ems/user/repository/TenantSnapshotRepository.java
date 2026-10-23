package com.ems.user.repository;

import com.ems.user.domain.TenantSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantSnapshotRepository extends JpaRepository<TenantSnapshot, UUID> {
}