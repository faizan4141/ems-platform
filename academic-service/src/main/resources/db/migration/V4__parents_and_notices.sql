-- Which Keycloak login belongs to which guardian email, per school. Filled from user-service events.
-- No foreign key on purpose: the event can arrive before the school's snapshot.
CREATE TABLE guardian_logins (
                                 keycloak_id UUID PRIMARY KEY,
                                 tenant_id   UUID         NOT NULL,
                                 email_lower VARCHAR(254) NOT NULL,
                                 updated_at  TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_guardian_logins_tenant_email ON guardian_logins (tenant_id, email_lower);
CREATE INDEX idx_guardians_email ON guardians (lower(email));

CREATE TABLE notices (
                         id           UUID PRIMARY KEY,
                         tenant_id    UUID         NOT NULL REFERENCES tenant_snapshots (tenant_id),
                         title        VARCHAR(200) NOT NULL,
                         body         TEXT         NOT NULL,
                         audience     VARCHAR(10)  NOT NULL,
                         published_at TIMESTAMPTZ  NOT NULL,
                         published_by VARCHAR(100) NOT NULL,
                         archived     BOOLEAN      NOT NULL DEFAULT FALSE,
                         CONSTRAINT ck_notice_audience CHECK (audience IN ('ALL', 'PARENTS', 'STAFF'))
);
CREATE INDEX idx_notices_tenant ON notices (tenant_id, archived, published_at DESC);