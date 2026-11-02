-- No foreign key to tenant_snapshots on purpose: the login event can arrive before the school's snapshot.
CREATE TABLE staff_members (
                               id          UUID PRIMARY KEY,
                               tenant_id   UUID         NOT NULL,
                               keycloak_id UUID         NOT NULL,
                               email_lower VARCHAR(254) NOT NULL,
                               first_name  VARCHAR(100) NOT NULL,
                               last_name   VARCHAR(100) NOT NULL,
                               designation VARCHAR(100),
                               active      BOOLEAN      NOT NULL DEFAULT TRUE,
                               created_at  TIMESTAMPTZ  NOT NULL,
                               CONSTRAINT uq_staff_login UNIQUE (keycloak_id)
);
CREATE INDEX idx_staff_tenant ON staff_members (tenant_id);

CREATE TABLE teaching_assignments (
                                      id            UUID PRIMARY KEY,
                                      tenant_id     UUID        NOT NULL,
                                      section_id    UUID        NOT NULL REFERENCES sections (id) ON DELETE CASCADE,
                                      staff_id      UUID        NOT NULL REFERENCES staff_members (id) ON DELETE CASCADE,
                                      subject_id    UUID REFERENCES subjects (id) ON DELETE RESTRICT,
                                      class_teacher BOOLEAN     NOT NULL DEFAULT FALSE,
                                      created_at    TIMESTAMPTZ NOT NULL,
    -- a class-teacher row has no subject; a subject row must have one
                                      CONSTRAINT ck_assignment_kind CHECK ((class_teacher AND subject_id IS NULL) OR (NOT class_teacher AND subject_id IS NOT NULL))
);
-- At most one class teacher per section, and no duplicate subject assignment: guaranteed by the database
CREATE UNIQUE INDEX uq_one_class_teacher ON teaching_assignments (section_id) WHERE class_teacher;
CREATE UNIQUE INDEX uq_subject_assignment ON teaching_assignments (section_id, staff_id, subject_id) WHERE subject_id IS NOT NULL;
CREATE INDEX idx_assignments_staff ON teaching_assignments (staff_id);