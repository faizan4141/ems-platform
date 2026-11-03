CREATE TABLE periods (
                         id           UUID PRIMARY KEY,
                         tenant_id    UUID        NOT NULL REFERENCES tenant_snapshots (tenant_id),
                         period_no    INT         NOT NULL CHECK (period_no BETWEEN 1 AND 20),
                         name         VARCHAR(30) NOT NULL,
                         start_time   TIME        NOT NULL,
                         end_time     TIME        NOT NULL,
                         break_period BOOLEAN     NOT NULL DEFAULT FALSE,
                         CONSTRAINT uq_period_no UNIQUE (tenant_id, period_no),
                         CONSTRAINT ck_period_times CHECK (end_time > start_time)
);

CREATE TABLE timetable_entries (
                                   id          UUID PRIMARY KEY,
                                   tenant_id   UUID        NOT NULL,
                                   section_id  UUID        NOT NULL REFERENCES sections (id) ON DELETE CASCADE,
                                   day_of_week INT         NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
                                   period_id   UUID        NOT NULL REFERENCES periods (id) ON DELETE RESTRICT,
                                   subject_id  UUID        NOT NULL REFERENCES subjects (id) ON DELETE RESTRICT,
                                   staff_id    UUID        NOT NULL REFERENCES staff_members (id) ON DELETE RESTRICT,
                                   room        VARCHAR(30),
                                   created_at  TIMESTAMPTZ NOT NULL,
    -- a section has one lesson per slot
                                   CONSTRAINT uq_slot_section UNIQUE (section_id, day_of_week, period_id)
);
-- A teacher cannot be in two places at once, and a room cannot hold two lessons: guaranteed by the database
CREATE UNIQUE INDEX uq_slot_teacher ON timetable_entries (staff_id, day_of_week, period_id);
CREATE UNIQUE INDEX uq_slot_room ON timetable_entries (tenant_id, lower(room), day_of_week, period_id) WHERE room IS NOT NULL;
CREATE INDEX idx_entries_staff ON timetable_entries (staff_id);