package com.ems.academic.repository;

import com.ems.academic.domain.PaymentAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    Optional<PaymentAttempt> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<PaymentAttempt> findByTenantIdAndIdempotencyKey(UUID tenantId, UUID key);
    Optional<PaymentAttempt> findByProviderRef(String providerRef);
    List<PaymentAttempt> findTop100ByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    /** Serializes webhook handling for one attempt, so two deliveries never apply the money twice. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PaymentAttempt a where a.providerRef = :ref")
    Optional<PaymentAttempt> lockByProviderRef(@Param("ref") String ref);

    @Query("select count(i) from PaymentAttemptItem i, PaymentAttempt a where i.attemptId = a.id "
            + "and a.status = com.ems.academic.domain.AttemptStatus.PENDING and a.createdAt > :since "
            + "and i.chargeId in :chargeIds")
    long countPendingForCharges(@Param("since") Instant since, @Param("chargeIds") Collection<UUID> chargeIds);
}