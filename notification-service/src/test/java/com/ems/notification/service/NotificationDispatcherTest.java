package com.ems.notification.service;

import com.ems.notification.domain.Channel;
import com.ems.notification.domain.Notification;
import com.ems.notification.domain.NotificationStatus;
import com.ems.notification.repository.NotificationRepository;
import com.ems.notification.sender.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    private static final Instant NOW = Instant.parse("2026-10-30T05:00:00Z");

    @Mock NotificationRepository repository;
    @Mock NotificationSender emailSender;
    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        when(emailSender.channel()).thenReturn(Channel.EMAIL);
        dispatcher = new NotificationDispatcher(repository, List.of(emailSender), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Notification email() {
        return new Notification(UUID.randomUUID(), UUID.randomUUID(), Channel.EMAIL, "raj@example.com", "Subject", "Body", NOW);
    }

    private void due(Notification n) {
        when(repository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                eq(NotificationStatus.PENDING), any())).thenReturn(List.of(n));
    }

    @Test
    void marksAsSentAfterDelivery() throws Exception {
        Notification n = email();
        due(n);

        dispatcher.dispatchDue();

        verify(emailSender).send(n);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        verify(repository).save(n);
    }

    @Test
    void schedulesARetryWithBackoffWhenDeliveryFails() throws Exception {
        Notification n = email();
        due(n);
        doThrow(new RuntimeException("smtp down")).when(emailSender).send(n);

        dispatcher.dispatchDue();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.getAttempts()).isEqualTo(1);
        assertThat(n.getNextAttemptAt()).isEqualTo(NOW.plus(Duration.ofMinutes(1)));
    }

    @Test
    void givesUpAfterTheMaximumNumberOfAttempts() throws Exception {
        Notification n = email();
        for (int i = 0; i < NotificationDispatcher.MAX_ATTEMPTS - 1; i++) {
            n.markFailed("earlier failure", NOW, NotificationDispatcher.MAX_ATTEMPTS, Duration.ZERO);
        }
        due(n);
        doThrow(new RuntimeException("still down")).when(emailSender).send(n);

        dispatcher.dispatchDue();

        assertThat(n.getStatus()).isEqualTo(NotificationStatus.GAVE_UP);
    }

    @Test
    void backoffGrowsAndCapsAtOneHour() {
        assertThat(NotificationDispatcher.delayAfterFailure(1)).isEqualTo(Duration.ofMinutes(1));
        assertThat(NotificationDispatcher.delayAfterFailure(2)).isEqualTo(Duration.ofMinutes(5));
        assertThat(NotificationDispatcher.delayAfterFailure(3)).isEqualTo(Duration.ofMinutes(15));
        assertThat(NotificationDispatcher.delayAfterFailure(4)).isEqualTo(Duration.ofHours(1));
    }
}