CREATE TABLE tenants (
                         id                  UUID PRIMARY KEY,
                         code                VARCHAR(50)  NOT NULL UNIQUE,
                         name                VARCHAR(200) NOT NULL,
                         org_type            VARCHAR(20)  NOT NULL,
                         status              VARCHAR(20)  NOT NULL,
                         contract_start_date DATE         NOT NULL,
                         contract_end_date   DATE         NOT NULL,
                         created_at          TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_tenants_status ON tenants (status);