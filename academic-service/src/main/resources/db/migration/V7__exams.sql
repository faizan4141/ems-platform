CREATE TABLE subjects (
                          id         UUID PRIMARY KEY,
                          tenant_id  UUID         NOT NULL REFERENCES tenant_snapshots (tenant_id),
                          name       VARCHAR(100) NOT NULL,
                          code       VARCHAR(20),
                          created_at TIMESTAMPTZ  NOT NULL
);
CREATE UNIQUE INDEX uq_subject_name ON subjects (tenant_id, lower(name));

CREATE TABLE grade_bands (
                             id          UUID PRIMARY KEY,
                             tenant_id   UUID         NOT NULL,
                             min_percent NUMERIC(5,2) NOT NULL CHECK (min_percent >= 0 AND min_percent <= 100),
                             grade       VARCHAR(10)  NOT NULL,
                             label       VARCHAR(50),
                             CONSTRAINT uq_band_min UNIQUE (tenant_id, min_percent)
);

CREATE TABLE exams (
                       id           UUID PRIMARY KEY,
                       tenant_id    UUID         NOT NULL REFERENCES tenant_snapshots (tenant_id),
                       class_id     UUID         NOT NULL REFERENCES classes (id) ON DELETE CASCADE,
                       name         VARCHAR(100) NOT NULL,
                       status       VARCHAR(10)  NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED')),
                       created_at   TIMESTAMPTZ  NOT NULL,
                       created_by   VARCHAR(100) NOT NULL,
                       published_at TIMESTAMPTZ,
                       published_by VARCHAR(100)
);
CREATE UNIQUE INDEX uq_exam_name ON exams (class_id, lower(name));
CREATE INDEX idx_exams_tenant_class ON exams (tenant_id, class_id);

CREATE TABLE exam_subjects (
                               id         UUID PRIMARY KEY,
                               exam_id    UUID          NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
                               subject_id UUID          NOT NULL REFERENCES subjects (id) ON DELETE RESTRICT,
                               max_marks  NUMERIC(6,2)  NOT NULL CHECK (max_marks > 0),
                               pass_marks NUMERIC(6,2)  NOT NULL CHECK (pass_marks >= 0),
                               CONSTRAINT uq_exam_subject UNIQUE (exam_id, subject_id),
                               CONSTRAINT ck_pass_le_max CHECK (pass_marks <= max_marks)
);

CREATE TABLE exam_marks (
                            id              UUID PRIMARY KEY,
                            tenant_id       UUID         NOT NULL,
                            exam_subject_id UUID         NOT NULL REFERENCES exam_subjects (id) ON DELETE CASCADE,
                            student_id      UUID         NOT NULL REFERENCES students (id) ON DELETE CASCADE,
                            marks           NUMERIC(6,2) CHECK (marks >= 0),
                            absent          BOOLEAN      NOT NULL DEFAULT FALSE,
                            updated_at      TIMESTAMPTZ  NOT NULL,
                            updated_by      VARCHAR(100) NOT NULL,
                            CONSTRAINT uq_mark UNIQUE (exam_subject_id, student_id),
                            CONSTRAINT ck_mark_or_absent CHECK ((absent AND marks IS NULL) OR (NOT absent AND marks IS NOT NULL))
);
CREATE INDEX idx_marks_student ON exam_marks (student_id);

CREATE TABLE exam_audit (
                            id        UUID PRIMARY KEY,
                            tenant_id UUID         NOT NULL,
                            exam_id   UUID         NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
                            actor     VARCHAR(100) NOT NULL,
                            action    VARCHAR(20)  NOT NULL,
                            detail    VARCHAR(300),
                            at        TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_exam_audit_exam ON exam_audit (exam_id, at);