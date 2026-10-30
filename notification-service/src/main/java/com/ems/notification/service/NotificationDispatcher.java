package com.ems.notification.service;

import com.ems.notification.domain.Notification;
import com.ems.notification.domain.NotificationStatus;
import com.ems.notification.repository.NotificationRepository;
import com.ems.notification.sender.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationDispatcher {

    static final int MAX_ATTEMPTS = 5;
    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationRepository repository;
    private final Map<com.ems.notification.domain.Channel, NotificationSender> senders = new EnumMap<>(com.ems.notification.domain.Channel.class);
    private final Clock clock;

    public NotificationDispatcher(NotificationRepository repository, List<NotificationSender> senders, Clock clock) {
        this.repository = repository;
        this.clock = clock;
        senders.forEach(s -> this.senders.put(s.channel(), s));
    }

    // Run ONE instance of this service until row locking is added; two instances would send twice.
    @Scheduled(initialDelayString = "${notifications.dispatch.interval-ms:5000}",
            fixedDelayString = "${notifications.dispatch.interval-ms:5000}")
    public void dispatchDue() {
        repository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                NotificationStatus.PENDING, clock.instant()).forEach(this::deliver);
    }

    void deliver(Notification n) {
        try {
            NotificationSender sender = senders.get(n.getChannel());
            if (sender == null) {
                throw new IllegalStateException("No sender for " + n.getChannel());
            }
            sender.send(n);
            n.markSent(clock.instant());
        } catch (Exception e) {
            log.warn("Notification {} ({}) failed on attempt {}: {}", n.getId(), n.getChannel(),
                    n.getAttempts() + 1, e.getMessage());
            n.markFailed(e.getMessage(), clock.instant(), MAX_ATTEMPTS, delayAfterFailure(n.getAttempts() + 1));
        }
        repository.save(n);
    }

    /** 1 minute, 5 minutes, 15 minutes, then hourly. */
    static Duration delayAfterFailure(int failedAttempts) {
        return switch (failedAttempts) {
            case 1 -> Duration.ofMinutes(1);
            case 2 -> Duration.ofMinutes(5);
            case 3 -> Duration.ofMinutes(15);
            default -> Duration.ofHours(1);
        };
    }
}