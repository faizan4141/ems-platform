package com.ems.user.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserEventPublisher {

    public static final String TOPIC = "ems.user.events";
    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final KafkaTemplate<String, Object> kafka;

    public UserEventPublisher(KafkaTemplate<String, Object> kafka) { this.kafka = kafka; }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(UserLoginEvent event) {
        kafka.send(TOPIC, event.keycloakId().toString(), event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Could not publish {} for login {}", event.eventType(), event.keycloakId(), ex);
            }
        });
    }
}