ALTER TABLE payments DROP CONSTRAINT ck_payment_method;
ALTER TABLE payments ADD CONSTRAINT ck_payment_method
    CHECK (method IN ('CASH', 'CHEQUE', 'UPI', 'BANK_TRANSFER', 'CARD', 'OTHER', 'ONLINE'));
ALTER TABLE payments ADD COLUMN attempt_id UUID;

CREATE TABLE payment_attempts (
                                  id              UUID PRIMARY KEY,
                                  tenant_id       UUID          NOT NULL,
                                  student_id      UUID          NOT NULL REFERENCES students (id) ON DELETE CASCADE,
                                  initiated_by    VARCHAR(100)  NOT NULL,
                                  amount          NUMERIC(12,2) NOT NULL CHECK (amount > 0),
                                  currency        VARCHAR(3)    NOT NULL,
                                  provider        VARCHAR(30)   NOT NULL,
                                  provider_ref    VARCHAR(100),
                                  checkout_url    VARCHAR(500),
                                  status          VARCHAR(10)   NOT NULL,
                                  failure_reason  VARCHAR(200),
                                  review_note     VARCHAR(300),
                                  idempotency_key UUID          NOT NULL,
                                  created_at      TIMESTAMPTZ   NOT NULL,
                                  completed_at    TIMESTAMPTZ,
                                  CONSTRAINT uq_attempt_key UNIQUE (tenant_id, idempotency_key),
                                  CONSTRAINT ck_attempt_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'REVIEW'))
);
CREATE UNIQUE INDEX uq_attempt_provider_ref ON payment_attempts (provider_ref) WHERE provider_ref IS NOT NULL;
CREATE INDEX idx_attempts_tenant_created ON payment_attempts (tenant_id, created_at DESC);

-- Which fees an attempt pays for, and how much of it each
CREATE TABLE payment_attempt_items (
                                       id         UUID PRIMARY KEY,
                                       attempt_id UUID          NOT NULL REFERENCES payment_attempts (id) ON DELETE CASCADE,
                                       charge_id  UUID          NOT NULL REFERENCES fee_charges (id),
                                       amount     NUMERIC(12,2) NOT NULL CHECK (amount > 0),
                                       CONSTRAINT uq_attempt_charge UNIQUE (attempt_id, charge_id)
);
CREATE INDEX idx_attempt_items_charge ON payment_attempt_items (charge_id);