import { useMemo, useState } from 'react';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { useAuth } from '../features/auth/useAuth';
import {
  STATUS_LABEL, STATUS_TONE, buildMarks, countStatuses, initialStatuses, isDirty, nextStatus, notifiedText,
} from '../app/attendance';
import { useSectionOptions } from '../app/useSectionOptions';
import { useGetRosterQuery, useSaveAttendanceMutation } from '../services/attendanceApi';
import { Avatar, Chip, EmptyState, ErrorNotice, Field, PageHeader } from '../components/ui';

function RosterEditor({ roster, date }) {
  const dispatch = useDispatch();
  const initial = useMemo(() => initialStatuses(roster.students), [roster]);
  const [statuses, setStatuses] = useState(initial);
  const [save, { isLoading, error }] = useSaveAttendanceMutation();

  const counts = countStatuses(statuses);
  const dirty = isDirty(initial, statuses);

  const cycle = (id) => setStatuses({ ...statuses, [id]: nextStatus(statuses[id]) });
  const markAllPresent = () => setStatuses(Object.fromEntries(Object.keys(statuses).map((id) => [id, 'PRESENT'])));

  const onSave = async () => {
    try {
      const result = await save({ sectionId: roster.sectionId, date, marks: buildMarks(statuses) }).unwrap();
      dispatch(pushToast({ text: notifiedText(result.notified) }));
    } catch { /* shown below */ }
  };

  if (roster.students.length === 0) {
    return <EmptyState title="No students in this section" text="Assign students to this section from the Students page." />;
  }

  return (
    <>
      <div className="summary-chips">
        <Chip tone="ok">Present {counts.PRESENT}</Chip>
        <Chip tone="bad">Absent {counts.ABSENT}</Chip>
        <Chip tone="warn">Late {counts.LATE}</Chip>
        <span className="spacer" />
        {roster.taken && !dirty && <Chip tone="info">Saved</Chip>}
        <button className="btn btn-secondary" onClick={markAllPresent}>Mark all present</button>
      </div>
      <p className="muted" style={{ marginTop: 0 }}>Everyone starts as present. Tap a student to change: Present → Absent → Late.</p>

      <ul className="roster">
        {roster.students.map((s) => (
          <li key={s.studentId}>
            <button className="roster-row" onClick={() => cycle(s.studentId)}
                    aria-label={`${s.name}, ${STATUS_LABEL[statuses[s.studentId]]}. Tap to change.`}>
              <Avatar name={s.name} />
              <span className="name"><strong>{s.name}</strong><span className="muted">{s.admissionNo}</span></span>
              <Chip tone={STATUS_TONE[statuses[s.studentId]]}>{STATUS_LABEL[statuses[s.studentId]]}</Chip>
            </button>
          </li>
        ))}
      </ul>

      <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginTop: 16 }}>
        <button className="btn btn-primary" disabled={isLoading || (roster.taken && !dirty)} onClick={onSave}>
          {isLoading ? 'Saving…' : roster.taken ? 'Update attendance' : 'Save attendance'}
        </button>
        {dirty && <span className="muted">Unsaved changes</span>}
      </div>
      <ErrorNotice error={error} />
    </>
  );
}

export default function AttendancePage() {
  const isAdmin = useAuth().roles.includes('TENANT_ADMIN');
  const { options, ready } = useSectionOptions();
  const [sectionId, setSectionId] = useState('');
  const [date, setDate] = useState('');           // empty means "today", decided by the server
  const selected = sectionId || options[0]?.id || '';

  const { currentData: roster, isFetching, error } = useGetRosterQuery(
    { sectionId: selected, date }, { skip: !selected });

  return (
    <>
      <PageHeader title="Attendance" subtitle={date ? `Showing ${date}` : 'Today'} />

      <section className="card">
        {options.length === 0 ? (
          <EmptyState title={ready && !isAdmin ? 'You are not assigned to any class yet' : 'No classes yet'}
            text={ready && !isAdmin ? 'Ask a school admin to assign you to a class.' : 'Create classes and sections in School setup first.'} />
        ) : (
          <>
            <div className="toolbar">
              <Field label="Class and section">
                <select value={selected} onChange={(e) => setSectionId(e.target.value)}>
                  {options.map((o) => <option key={o.id} value={o.id}>{o.label}</option>)}
                </select>
              </Field>
              {isAdmin && (
                <Field label="Date (admins can correct past days)">
                  <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
                </Field>
              )}
            </div>
            <ErrorNotice error={error} />
            {isFetching && !roster && <div className="skeleton" style={{ width: '50%' }} />}
            {roster && <RosterEditor key={`${roster.sectionId}|${roster.date}`} roster={roster} date={date} />}
          </>
        )}
      </section>
    </>
  );
}