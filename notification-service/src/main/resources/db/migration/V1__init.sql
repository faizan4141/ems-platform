CREATE TABLE notifications (
                               id              UUID PRIMARY KEY,
                               tenant_id       UUID         NOT NULL,
                               event_id        UUID         NOT NULL,
                               channel         VARCHAR(10)  NOT NULL,
                               recipient       VARCHAR(254) NOT NULL,
                               subject         VARCHAR(200),
                               body            TEXT         NOT NULL,
                               status          VARCHAR(10)  NOT NULL,
                               attempts        INT          NOT NULL DEFAULT 0,
                               last_error      VARCHAR(500),
                               created_at      TIMESTAMPTZ  NOT NULL,
                               sent_at         TIMESTAMPTZ,
                               next_attempt_at TIMESTAMPTZ  NOT NULL,
    -- the same event can never produce two messages on the same channel
                               CONSTRAINT uq_notification_event_channel UNIQUE (event_id, channel),
                               CONSTRAINT ck_notification_status CHECK (status IN ('PENDING', 'SENT', 'GAVE_UP'))
);
CREATE INDEX idx_notifications_due ON notifications (status, next_attempt_at);