CREATE TABLE fee_items (
                           id         UUID PRIMARY KEY,
                           tenant_id  UUID          NOT NULL REFERENCES tenant_snapshots (tenant_id),
                           class_id   UUID          NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
                           name       VARCHAR(100)  NOT NULL,
                           amount     NUMERIC(12,2) NOT NULL CHECK (amount > 0),
                           due_date   DATE          NOT NULL,
                           created_at TIMESTAMPTZ   NOT NULL
);
CREATE UNIQUE INDEX uq_fee_item_name ON fee_items (class_id, lower(name));

-- One row per student per fee item. Name, amount and due date are copied from the item,
-- so changing the setup later never rewrites what a family was billed.
CREATE TABLE fee_charges (
                             id               UUID PRIMARY KEY,
                             tenant_id        UUID          NOT NULL,
                             student_id       UUID          NOT NULL REFERENCES students (id) ON DELETE CASCADE,
                             fee_item_id      UUID          NOT NULL REFERENCES fee_items (id) ON DELETE RESTRICT,
                             name             VARCHAR(100)  NOT NULL,
                             amount           NUMERIC(12,2) NOT NULL CHECK (amount > 0),
                             due_date         DATE          NOT NULL,
                             last_reminded_at TIMESTAMPTZ,
                             created_at       TIMESTAMPTZ   NOT NULL,
                             CONSTRAINT uq_charge_student_item UNIQUE (student_id, fee_item_id)
);
CREATE INDEX idx_charges_tenant_due ON fee_charges (tenant_id, due_date);
CREATE INDEX idx_charges_student ON fee_charges (student_id);

CREATE TABLE payments (
                          id              UUID PRIMARY KEY,
                          tenant_id       UUID          NOT NULL,
                          charge_id       UUID          NOT NULL REFERENCES fee_charges (id),
                          amount          NUMERIC(12,2) NOT NULL CHECK (amount > 0),
                          method          VARCHAR(20)   NOT NULL,
                          reference       VARCHAR(100),
                          paid_on         DATE          NOT NULL,
                          receipt_no      VARCHAR(30)   NOT NULL,
                          recorded_by     VARCHAR(100)  NOT NULL,
                          recorded_at     TIMESTAMPTZ   NOT NULL,
                          idempotency_key UUID          NOT NULL,
                          voided          BOOLEAN       NOT NULL DEFAULT FALSE,
                          void_reason     VARCHAR(200),
                          voided_by       VARCHAR(100),
                          voided_at       TIMESTAMPTZ,
                          CONSTRAINT uq_receipt_no UNIQUE (tenant_id, receipt_no),
                          CONSTRAINT uq_payment_key UNIQUE (tenant_id, idempotency_key),
                          CONSTRAINT ck_payment_method CHECK (method IN ('CASH', 'CHEQUE', 'UPI', 'BANK_TRANSFER', 'CARD', 'OTHER'))
);
CREATE INDEX idx_payments_charge ON payments (charge_id);

-- Per-school receipt counter: the row lock also serializes payment recording per school
CREATE TABLE receipt_counters (
                                  tenant_id UUID PRIMARY KEY,
                                  last_no   BIGINT NOT NULL
);