package com.ems.academic.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ReceiptNumbers {

    @PersistenceContext
    private EntityManager em;

    /** The upsert locks this school's counter row until the surrounding transaction ends, so numbers never repeat. */
    public String next(UUID tenantId) {
        em.createNativeQuery("insert into academic.receipt_counters as rc (tenant_id, last_no) values (:t, 1) "
                        + "on conflict (tenant_id) do update set last_no = rc.last_no + 1")
                .setParameter("t", tenantId).executeUpdate();
        Number number = (Number) em.createNativeQuery(
                        "select last_no from academic.receipt_counters where tenant_id = :t")
                .setParameter("t", tenantId).getSingleResult();
        return format(number.longValue());
    }

    static String format(long number) { return String.format("RCP-%06d", number); }
}