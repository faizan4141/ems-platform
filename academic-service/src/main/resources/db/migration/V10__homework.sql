CREATE TABLE homework (
                          id           UUID PRIMARY KEY,
                          tenant_id    UUID         NOT NULL REFERENCES tenant_snapshots (tenant_id),
                          section_id   UUID         NOT NULL REFERENCES sections (id) ON DELETE CASCADE,
                          subject_id   UUID         NOT NULL REFERENCES subjects (id) ON DELETE RESTRICT,
                          kind         VARCHAR(10)  NOT NULL CHECK (kind IN ('HOMEWORK', 'NOTE')),
                          title        VARCHAR(200) NOT NULL,
                          description  TEXT         NOT NULL,
                          due_date     DATE,
                          links        TEXT,
                          author_login VARCHAR(100) NOT NULL,
                          author_name  VARCHAR(100) NOT NULL,
                          created_at   TIMESTAMPTZ  NOT NULL,
                          updated_at   TIMESTAMPTZ  NOT NULL,
                          archived     BOOLEAN      NOT NULL DEFAULT FALSE,
    -- homework always has a due date, a note never does
                          CONSTRAINT ck_homework_due CHECK ((kind = 'HOMEWORK' AND due_date IS NOT NULL) OR (kind = 'NOTE' AND due_date IS NULL))
);
CREATE INDEX idx_homework_section ON homework (section_id, archived, created_at DESC);
CREATE INDEX idx_homework_tenant ON homework (tenant_id, archived, created_at DESC);