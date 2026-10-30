package com.ems.notification;

import com.ems.notification.domain.Channel;
import com.ems.notification.domain.Notification;
import com.ems.notification.event.AttendanceAlertEvent;
import com.ems.notification.repository.NotificationRepository;
import com.ems.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {"spring.kafka.listener.auto-startup=false", "notifications.dispatch.interval-ms=3600000"})
class NotificationIdempotencyIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings({"rawtypes", "resource"})
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @Autowired NotificationService service;
    @Autowired NotificationRepository repository;

    private AttendanceAlertEvent event(String email) {
        return new AttendanceAlertEvent(UUID.randomUUID(), "STUDENT_ABSENT", UUID.randomUUID(), "School",
                UUID.randomUUID(), "Aarav Sharma", "Class 8", "A", LocalDate.of(2026, 10, 30),
                "Raj Sharma", "+919876543210", email, Instant.now());
    }

    private List<Notification> rowsFor(AttendanceAlertEvent e) {
        return repository.findAll().stream().filter(n -> n.getEventId().equals(e.eventId())).toList();
    }

    @Test
    void theSameEventNeverCreatesTheMessagesTwice() {
        AttendanceAlertEvent e = event("raj@example.com");

        assertThat(service.handle(e)).isEqualTo(2);
        assertThat(service.handle(e)).isZero();

        assertThat(rowsFor(e)).extracting(Notification::getChannel).containsExactlyInAnyOrder(Channel.EMAIL, Channel.SMS);
    }

    @Test
    void aGuardianWithoutEmailGetsOnlyAnSms() {
        AttendanceAlertEvent e = event(null);

        service.handle(e);

        assertThat(rowsFor(e)).extracting(Notification::getChannel).containsExactly(Channel.SMS);
    }
}