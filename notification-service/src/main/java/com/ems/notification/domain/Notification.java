package com.ems.notification.domain;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false) private UUID tenantId;
    @Column(name = "event_id", nullable = false, updatable = false) private UUID eventId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10, updatable = false) private Channel channel;
    @Column(nullable = false, length = 254, updatable = false) private String recipient;
    @Column(length = 200, updatable = false) private String subject;
    @Column(nullable = false, columnDefinition = "text", updatable = false) private String body;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private NotificationStatus status;
    @Column(nullable = false) private int attempts;
    @Column(name = "last_error", length = 500) private String lastError;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;

    protected Notification() {}

    public Notification(UUID tenantId, UUID eventId, Channel channel, String recipient,
                        String subject, String body, Instant now) {
        this.tenantId = tenantId;
        this.eventId = eventId;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.status = NotificationStatus.PENDING;
        this.createdAt = now;
        this.nextAttemptAt = now;
    }

    public void markSent(Instant now) {
        this.status = NotificationStatus.SENT;
        this.sentAt = now;
        this.attempts++;
        this.lastError = null;
    }

    public void markFailed(String error, Instant now, int maxAttempts, Duration retryDelay) {
        this.attempts++;
        String message = error == null ? "unknown error" : error;
        this.lastError = message.length() > 500 ? message.substring(0, 500) : message;
        if (attempts >= maxAttempts) {
            this.status = NotificationStatus.GAVE_UP;
        } else {
            this.nextAttemptAt = now.plus(retryDelay);
        }
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public Channel getChannel() { return channel; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public NotificationStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
}