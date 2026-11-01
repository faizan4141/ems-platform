package com.ems.academic.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "ems.kafka.publish", havingValue = "true", matchIfMissing = true)
public class FeeEventPublisher {

    public static final String TOPIC = "ems.fee.events";
    private static final Logger log = LoggerFactory.getLogger(FeeEventPublisher.class);

    private final KafkaTemplate<String, Object> kafka;

    public FeeEventPublisher(KafkaTemplate<String, Object> kafka) { this.kafka = kafka; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(FeeReminderEvent event) {
        kafka.send(TOPIC, event.studentId().toString(), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Could not publish {} for student {}", event.eventType(), event.studentId(), ex);
            }
        });
    }
}