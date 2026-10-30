CREATE TABLE attendance_sessions (
                                     id              UUID PRIMARY KEY,
                                     tenant_id       UUID         NOT NULL,
                                     section_id      UUID         NOT NULL REFERENCES sections (id) ON DELETE CASCADE,
                                     attendance_date DATE         NOT NULL,
                                     taken_by        VARCHAR(100) NOT NULL,
                                     taken_at        TIMESTAMPTZ  NOT NULL,
                                     updated_at      TIMESTAMPTZ  NOT NULL,
                                     CONSTRAINT uq_attendance_session UNIQUE (section_id, attendance_date)
);
CREATE INDEX idx_attendance_sessions_tenant_date ON attendance_sessions (tenant_id, attendance_date);

CREATE TABLE attendance_records (
                                    id         UUID PRIMARY KEY,
                                    tenant_id  UUID        NOT NULL,
                                    session_id UUID        NOT NULL REFERENCES attendance_sessions (id) ON DELETE CASCADE,
                                    student_id UUID        NOT NULL REFERENCES students (id) ON DELETE CASCADE,
                                    status     VARCHAR(10) NOT NULL,
                                    CONSTRAINT uq_attendance_record UNIQUE (session_id, student_id),
                                    CONSTRAINT ck_attendance_status CHECK (status IN ('PRESENT', 'ABSENT', 'LATE'))
);
CREATE INDEX idx_attendance_records_student ON attendance_records (student_id);