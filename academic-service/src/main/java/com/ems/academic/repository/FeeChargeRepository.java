package com.ems.academic.repository;

import com.ems.academic.domain.FeeCharge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeeChargeRepository extends JpaRepository<FeeCharge, UUID> {

    /** Locks the row until the transaction ends, so two payments cannot both pass the balance check. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from FeeCharge c where c.id = :id and c.tenantId = :tenantId")
    Optional<FeeCharge> lockById(@Param("id") UUID id, @Param("tenantId") UUID tenantId);

    List<FeeCharge> findByTenantId(UUID tenantId);
    List<FeeCharge> findByTenantIdAndStudentIdOrderByDueDateAscNameAsc(UUID tenantId, UUID studentId);
    boolean existsByFeeItemId(UUID feeItemId);

    @Query("select c.studentId, c.feeItemId from FeeCharge c where c.tenantId = :tenantId and c.feeItemId in :itemIds")
    List<Object[]> findExisting(@Param("tenantId") UUID tenantId, @Param("itemIds") Collection<UUID> itemIds);

    @Query("select c.feeItemId, count(c) from FeeCharge c where c.tenantId = :tenantId group by c.feeItemId")
    List<Object[]> countsByItem(@Param("tenantId") UUID tenantId);

    @Query("select c from FeeCharge c where c.tenantId = :tenantId and c.dueDate <= :cutoff "
            + "and (c.lastRemindedAt is null or c.lastRemindedAt < :threshold)")
    List<FeeCharge> findReminderCandidates(@Param("tenantId") UUID tenantId, @Param("cutoff") LocalDate cutoff,
                                           @Param("threshold") Instant threshold);
}