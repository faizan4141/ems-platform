package com.ems.tenant.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TenantEventPublisher {

    public static final String TOPIC = "ems.tenant.events";
    private static final Logger log = LoggerFactory.getLogger(TenantEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TenantEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // Runs only AFTER the database transaction commits successfully.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(TenantEvent event) {
        kafkaTemplate.send(TOPIC, event.tenantId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} for tenant {}",
                                event.eventType(), event.tenantId(), ex);
                    } else {
                        log.info("Published {} for tenant {} to partition {}",
                                event.eventType(), event.tenantId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}