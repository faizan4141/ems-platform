package com.ems.academic.repository;

import com.ems.academic.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Payment> findByTenantIdAndIdempotencyKey(UUID tenantId, UUID key);
    List<Payment> findByChargeIdInOrderByRecordedAtDesc(Collection<UUID> chargeIds);

    /** Null when nothing has been paid. Voided payments never count. */
    @Query("select sum(p.amount) from Payment p where p.chargeId = :chargeId and p.voided = false")
    BigDecimal sumForCharge(@Param("chargeId") UUID chargeId);

    @Query("select p.chargeId, sum(p.amount) from Payment p where p.chargeId in :ids and p.voided = false group by p.chargeId")
    List<Object[]> sumByCharge(@Param("ids") Collection<UUID> ids);

    @Query("select p.chargeId, sum(p.amount) from Payment p where p.tenantId = :tenantId and p.voided = false group by p.chargeId")
    List<Object[]> sumByChargeForTenant(@Param("tenantId") UUID tenantId);
}