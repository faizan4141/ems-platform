package com.ems.academic.repository;

import com.ems.academic.domain.TenantSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantSnapshotRepository extends JpaRepository<TenantSnapshot, UUID> {
}