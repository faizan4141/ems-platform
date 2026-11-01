package com.ems.academic.service;

import com.ems.academic.domain.AttemptStatus;
import com.ems.academic.domain.PaymentAttempt;
import com.ems.academic.domain.Student;
import com.ems.academic.dto.OnlinePaymentDtos.*;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.PaymentGatewayException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.payment.CheckoutRequest;
import com.ems.academic.payment.CheckoutSession;
import com.ems.academic.payment.PaymentGateway;
import com.ems.academic.repository.PaymentAttemptRepository;
import com.ems.academic.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OnlinePaymentService {

    private final TenantGuard guard;
    private final ParentService parents;
    private final PaymentAttemptStore store;
    private final PaymentGateway gateway;
    private final PaymentAttemptRepository attempts;
    private final StudentRepository students;

    public OnlinePaymentService(TenantGuard guard, ParentService parents, PaymentAttemptStore store,
                                PaymentGateway gateway, PaymentAttemptRepository attempts, StudentRepository students) {
        this.guard = guard;
        this.parents = parents;
        this.store = store;
        this.gateway = gateway;
        this.attempts = attempts;
        this.students = students;
    }

    // Deliberately NOT transactional: the provider call must not hold a database connection.
    public CheckoutResponse start(UUID tenantId, String subject, UUID studentId, PayRequest request) {
        guard.requireActive(tenantId);
        parents.requireChild(tenantId, subject, studentId);   // a parent can only pay for their own children

        PaymentAttemptStore.StartResult started = store.create(
                tenantId, studentId, subject, request.chargeIds(), request.idempotencyKey(), gateway.name());
        PaymentAttempt attempt = started.attempt();

        if (started.existing()) {
            if (attempt.getStatus() != AttemptStatus.PENDING) {
                throw new DuplicateResourceException("This payment request was already used");
            }
            if (attempt.getCheckoutUrl() == null) {
                throw new InvalidRequestException("This payment is still being prepared. Please try again in a moment.");
            }
            return new CheckoutResponse(attempt.getId(), attempt.getCheckoutUrl());
        }

        try {
            CheckoutSession session = gateway.createCheckout(new CheckoutRequest(
                    attempt.getId(), attempt.getAmount(), attempt.getCurrency(), started.description()));
            store.attach(attempt.getId(), session.providerRef(), session.checkoutUrl());
            return new CheckoutResponse(attempt.getId(), session.checkoutUrl());
        } catch (RuntimeException e) {
            store.failSetup(attempt.getId(), "Could not reach the payment provider");
            throw new PaymentGatewayException("Could not create the checkout", e);
        }
    }

    @Transactional(readOnly = true)
    public AttemptView status(UUID tenantId, String subject, UUID attemptId) {
        return attempts.findByIdAndTenantId(attemptId, tenantId)
                .filter(a -> a.getInitiatedBy().equals(subject))
                .map(AttemptView::from)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
    }

    @Transactional(readOnly = true)
    public List<AttemptRow> listForAdmin(UUID tenantId) {
        guard.requireActive(tenantId);
        List<PaymentAttempt> recent = attempts.findTop100ByTenantIdOrderByCreatedAtDesc(tenantId);
        Map<UUID, Student> known = students.findAllById(
                        recent.stream().map(PaymentAttempt::getStudentId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        return recent.stream().map(a -> {
            Student s = known.get(a.getStudentId());
            return new AttemptRow(a.getId(), a.getStudentId(),
                    s == null ? "Unknown" : s.getFirstName() + " " + s.getLastName(),
                    s == null ? "" : s.getAdmissionNo(), a.getStatus(), a.getAmount(), a.getCurrency(),
                    a.getCreatedAt(), a.getReviewNote() != null ? a.getReviewNote() : a.getFailureReason());
        }).toList();
    }
}