-- Local copy of tenant data, kept current from Kafka events
CREATE TABLE tenant_snapshots (
                                  tenant_id     UUID PRIMARY KEY,
                                  code          VARCHAR(50)  NOT NULL,
                                  name          VARCHAR(200) NOT NULL,
                                  status        VARCHAR(20)  NOT NULL,
                                  last_event_at TIMESTAMPTZ  NOT NULL
);

CREATE TABLE school_profiles (
                                 tenant_id      UUID PRIMARY KEY REFERENCES tenant_snapshots (tenant_id),
                                 display_name   VARCHAR(200) NOT NULL,
                                 board          VARCHAR(50),
                                 affiliation_no VARCHAR(50),
                                 address_line   VARCHAR(300),
                                 city           VARCHAR(100),
                                 state          VARCHAR(100),
                                 postal_code    VARCHAR(20),
                                 phone          VARCHAR(30),
                                 email          VARCHAR(254),
                                 website        VARCHAR(300),
                                 updated_at     TIMESTAMPTZ NOT NULL
);

CREATE TABLE academic_years (
                                id         UUID PRIMARY KEY,
                                tenant_id  UUID        NOT NULL REFERENCES tenant_snapshots (tenant_id),
                                name       VARCHAR(50) NOT NULL,
                                start_date DATE        NOT NULL,
                                end_date   DATE        NOT NULL,
                                is_current BOOLEAN     NOT NULL DEFAULT FALSE,
                                created_at TIMESTAMPTZ NOT NULL,
                                CONSTRAINT uq_year_name UNIQUE (tenant_id, name),
                                CONSTRAINT ck_year_dates CHECK (end_date > start_date)
);
-- At most ONE current year per school, guaranteed by the database itself
CREATE UNIQUE INDEX uq_one_current_year ON academic_years (tenant_id) WHERE is_current;

CREATE TABLE classes (
                         id               UUID PRIMARY KEY,
                         tenant_id        UUID        NOT NULL,
                         academic_year_id UUID        NOT NULL REFERENCES academic_years (id) ON DELETE CASCADE,
                         name             VARCHAR(50) NOT NULL,
                         grade_level      INT         NOT NULL,
                         created_at       TIMESTAMPTZ NOT NULL,
                         CONSTRAINT uq_class_name UNIQUE (academic_year_id, name),
                         CONSTRAINT ck_grade CHECK (grade_level BETWEEN 0 AND 12)
);
CREATE INDEX idx_classes_tenant ON classes (tenant_id);

CREATE TABLE sections (
                          id        UUID PRIMARY KEY,
                          tenant_id UUID        NOT NULL,
                          class_id  UUID        NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
                          name      VARCHAR(20) NOT NULL,
                          capacity  INT,
                          CONSTRAINT uq_section_name UNIQUE (class_id, name),
                          CONSTRAINT ck_capacity CHECK (capacity IS NULL OR capacity > 0)
);