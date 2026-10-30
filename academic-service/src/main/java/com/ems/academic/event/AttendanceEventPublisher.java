package com.ems.academic.event;

import com.ems.academic.domain.AttendanceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "ems.kafka.publish", havingValue = "true", matchIfMissing = true)
public class AttendanceEventPublisher {

    public static final String TOPIC = "ems.attendance.events";
    private static final Logger log = LoggerFactory.getLogger(AttendanceEventPublisher.class);

    private final KafkaTemplate<String, Object> kafka;

    public AttendanceEventPublisher(KafkaTemplate<String, Object> kafka) { this.kafka = kafka; }

    /** Runs only after the attendance transaction commits. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(AttendanceRecorded recorded) {
        for (AttendanceAlertEvent message : toMessages(recorded, Instant.now())) {
            kafka.send(TOPIC, message.studentId().toString(), message).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Could not publish {} for student {}", message.eventType(), message.studentId(), ex);
                }
            });
        }
    }

    public static List<AttendanceAlertEvent> toMessages(AttendanceRecorded e, Instant now) {
        return e.alerts().stream().map(a -> new AttendanceAlertEvent(
                UUID.randomUUID(),
                a.status() == AttendanceStatus.ABSENT ? "STUDENT_ABSENT" : "STUDENT_LATE",
                e.tenantId(), e.schoolName(), a.studentId(), a.studentName(), e.className(), e.sectionName(),
                e.date(), a.guardianName(), a.guardianPhone(), a.guardianEmail(), now)).toList();
    }
}