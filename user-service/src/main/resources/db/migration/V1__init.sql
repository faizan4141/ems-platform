-- Local read-copy of tenant data, kept up to date from Kafka events
CREATE TABLE tenant_snapshots
(
    tenant_id     UUID PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL,
    name          VARCHAR(200) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    last_event_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE users
(
    id         UUID PRIMARY KEY,
    tenant_id  UUID         NOT NULL REFERENCES tenant_snapshots (tenant_id),
    email      VARCHAR(254) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email)
);

CREATE INDEX idx_users_tenant ON users (tenant_id);