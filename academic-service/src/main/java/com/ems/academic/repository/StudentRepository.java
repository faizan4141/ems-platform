package com.ems.academic.repository;

import com.ems.academic.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByTenantId(UUID tenantId);
    boolean existsByTenantIdAndAdmissionNoIgnoreCase(UUID tenantId, String admissionNo);

    @Query("select lower(s.admissionNo) from Student s where s.tenantId = :tenantId")
    List<String> findAdmissionNosLower(@Param("tenantId") UUID tenantId);
}