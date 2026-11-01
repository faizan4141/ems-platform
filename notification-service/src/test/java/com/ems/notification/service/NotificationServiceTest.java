package com.ems.notification.service;

import com.ems.notification.domain.Channel;
import com.ems.notification.domain.Notification;
import com.ems.notification.event.AttendanceAlertEvent;
import com.ems.notification.repository.NotificationRepository;
import com.ems.notification.event.FeeReminderEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository repository;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-30T05:00:00Z"), ZoneOffset.UTC);
        service = new NotificationService(repository, clock, "UniTent");
    }

    private AttendanceAlertEvent event(String email) {
        return new AttendanceAlertEvent(UUID.randomUUID(), "STUDENT_ABSENT", UUID.randomUUID(), "School",
                UUID.randomUUID(), "Aarav Sharma", "Class 8", "A", LocalDate.of(2026, 10, 30),
                "Raj Sharma", "+919876543210", email, Instant.now());
    }

    @SuppressWarnings("unchecked")
    private List<Notification> savedRows() {
        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        return captor.getValue();
    }

    private FeeReminderEvent feeEvent(String email) {
        return new FeeReminderEvent(UUID.randomUUID(), "FEE_OVERDUE", UUID.randomUUID(), "School", UUID.randomUUID(),
                "Aarav Sharma", "Class 8", "A", new BigDecimal("500.00"), "INR", 1, LocalDate.of(2026, 10, 20), true,
                "Raj Sharma", "+919876543210", email, Instant.now());
    }

    @Test
    void createsEmailAndSmsWhenAnEmailIsKnown() {
        assertThat(service.handle(event("raj@example.com"))).isEqualTo(2);
        assertThat(savedRows()).extracting(Notification::getChannel).containsExactly(Channel.EMAIL, Channel.SMS);
    }

    @Test
    void createsOnlyAnSmsWhenThereIsNoEmail() {
        assertThat(service.handle(event(null))).isEqualTo(1);
        assertThat(savedRows()).extracting(Notification::getChannel).containsExactly(Channel.SMS);
    }

    @Test
    void ignoresAnEventThatWasAlreadyHandled() {
        AttendanceAlertEvent e = event("raj@example.com");
        when(repository.existsByEventId(e.eventId())).thenReturn(true);

        assertThat(service.handle(e)).isZero();
        verify(repository, never()).saveAll(any());
    }

    @Test
    void feeRemindersCreateEmailAndSms() {
        assertThat(service.handleFee(feeEvent("raj@example.com"))).isEqualTo(2);
        assertThat(savedRows()).extracting(Notification::getChannel).containsExactly(Channel.EMAIL, Channel.SMS);
    }

    @Test
    void aRepeatedFeeReminderIsIgnored() {
        FeeReminderEvent e = feeEvent("raj@example.com");
        when(repository.existsByEventId(e.eventId())).thenReturn(true);

        assertThat(service.handleFee(e)).isZero();
        verify(repository, never()).saveAll(any());
    }
}