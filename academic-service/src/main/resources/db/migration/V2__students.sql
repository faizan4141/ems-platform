CREATE TABLE students (
                          id             UUID PRIMARY KEY,
                          tenant_id      UUID         NOT NULL REFERENCES tenant_snapshots (tenant_id),
                          admission_no   VARCHAR(30)  NOT NULL,
                          first_name     VARCHAR(100) NOT NULL,
                          last_name      VARCHAR(100) NOT NULL,
                          date_of_birth  DATE         NOT NULL,
                          gender         VARCHAR(10),
                          admission_date DATE         NOT NULL,
                          section_id     UUID REFERENCES sections (id) ON DELETE SET NULL,
                          status         VARCHAR(20)  NOT NULL,
                          created_at     TIMESTAMPTZ  NOT NULL,
                          updated_at     TIMESTAMPTZ  NOT NULL,
                          CONSTRAINT uq_student_admission UNIQUE (tenant_id, admission_no)
);
CREATE INDEX idx_students_tenant_section ON students (tenant_id, section_id);
CREATE INDEX idx_students_tenant_name ON students (tenant_id, lower(last_name), lower(first_name));

CREATE TABLE guardians (
                           id           UUID PRIMARY KEY,
                           tenant_id    UUID         NOT NULL,
                           student_id   UUID         NOT NULL REFERENCES students (id) ON DELETE CASCADE,
                           name         VARCHAR(200) NOT NULL,
                           relationship VARCHAR(20)  NOT NULL,
                           phone        VARCHAR(20)  NOT NULL,
                           email        VARCHAR(254),
                           is_primary   BOOLEAN      NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_guardians_student ON guardians (student_id);
-- Exactly one primary contact per student at most, guaranteed by the database
CREATE UNIQUE INDEX uq_primary_guardian ON guardians (student_id) WHERE is_primary;