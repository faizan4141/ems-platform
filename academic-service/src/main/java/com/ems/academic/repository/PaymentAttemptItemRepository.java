package com.ems.academic.repository;

import com.ems.academic.domain.PaymentAttemptItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentAttemptItemRepository extends JpaRepository<PaymentAttemptItem, UUID> {
    List<PaymentAttemptItem> findByAttemptId(UUID attemptId);
}