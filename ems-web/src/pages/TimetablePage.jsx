import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { useAuth } from '../features/auth/useAuth';
import { useSectionOptions } from '../app/useSectionOptions';
import { indexEntries, indexLessons, slotKey, subjectsFor, teachersFor, timeRange } from '../app/timetable';
import { useListStaffQuery } from '../services/staffApi';
import {
  useClearSlotMutation, useGetMyTimetableQuery, useGetSectionTimetableQuery, useSetSlotMutation,
} from '../services/timetableApi';
import WeekGrid from '../components/WeekGrid';
import { EmptyState, ErrorNotice, Field, PageHeader } from '../components/ui';

// Mounted fresh for every slot (see the key below), so its form always starts from that slot's lesson.
function SlotDialog({ sectionId, slot, entry, staff, onClose }) {
  const dispatch = useDispatch();
  const ref = useRef(null);
  const [setSlot, setState] = useSetSlotMutation();
  const [clearSlot, clearState] = useClearSlotMutation();
  const [subjectId, setSubjectId] = useState(entry?.subjectId ?? '');
  const [staffId, setStaffId] = useState(entry?.staffId ?? '');
  const [room, setRoom] = useState(entry?.room ?? '');

  useEffect(() => { ref.current?.showModal(); }, []);

  const subjects = subjectsFor(staff, sectionId);
  const teachers = subjectId ? teachersFor(staff, sectionId, subjectId) : [];
  const where = { sectionId, day: slot.day, periodId: slot.period.id };

  const save = async (e) => {
    e.preventDefault();
    try {
      await setSlot({ ...where, subjectId, staffId, room: room.trim() || null }).unwrap();
      dispatch(pushToast({ text: 'Lesson saved' }));
      onClose();
    } catch { /* shown below */ }
  };

  const remove = async () => {
    try {
      await clearSlot(where).unwrap();
      dispatch(pushToast({ text: 'Lesson cleared' }));
      onClose();
    } catch { /* shown below */ }
  };

  return (
    <dialog ref={ref} className="dialog" onClose={onClose} aria-labelledby="slot-title">
      <form onSubmit={save}>
        <h2 id="slot-title">{slot.dayLabel}, {slot.period.name} <span className="muted">({timeRange(slot.period)})</span></h2>
        {subjects.length === 0 ? (
          <p className="muted">No teacher is assigned to a subject in this section yet. Assign teachers on the <Link to="/staff">Staff page</Link> first.</p>
        ) : (
          <div style={{ display: 'grid', gap: 16 }}>
            <Field label="Subject">
              <select value={subjectId} onChange={(e) => { setSubjectId(e.target.value); setStaffId(''); }} required>
                <option value="" disabled>Select…</option>
                {subjects.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </Field>
            <Field label="Teacher">
              <select value={staffId} onChange={(e) => setStaffId(e.target.value)} required disabled={!subjectId}>
                <option value="" disabled>Select…</option>
                {teachers.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
              </select>
            </Field>
            <Field label="Room (optional)"><input value={room} onChange={(e) => setRoom(e.target.value)} maxLength={30} placeholder="R101" /></Field>
          </div>
        )}
        <ErrorNotice error={setState.error || clearState.error} />
        <div className="dialog-actions">
          {entry && <button type="button" className="btn btn-secondary" disabled={clearState.isLoading} onClick={remove}>Clear lesson</button>}
          <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={setState.isLoading || !subjectId || !staffId}>{setState.isLoading ? 'Saving…' : 'Save'}</button>
        </div>
      </form>
    </dialog>
  );
}

function SectionView({ sectionId, editable }) {
  const { data, error } = useGetSectionTimetableQuery(sectionId);
  const { data: staff = [] } = useListStaffQuery(undefined, { skip: !editable });
  const [slot, setSlotTarget] = useState(null);
  if (error) return <ErrorNotice error={error} />;
  if (!data) return <div className="skeleton" style={{ width: '60%' }} />;
  if (data.periods.length === 0) {
    return <EmptyState title="No periods yet" text={editable ? 'Set up the school day first, then build the timetable.' : 'The school has not set up the timetable yet.'} />;
  }
  const entries = indexEntries(data.entries);

  return (
    <>
      <WeekGrid periods={data.periods}
        renderCell={(day, p) => {
          const e = entries[slotKey(day, p.id)];
          return e ? <><strong>{e.subjectName}</strong><small>{e.teacherName}</small>{e.room && <small>Room {e.room}</small>}</> : null;
        }}
        onCellClick={editable ? (day, period) => setSlotTarget({ day, period, dayLabel: ['', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'][day] }) : undefined} />
      {slot && <SlotDialog key={slotKey(slot.day, slot.period.id)} sectionId={sectionId} slot={slot} staff={staff}
                           entry={entries[slotKey(slot.day, slot.period.id)]} onClose={() => setSlotTarget(null)} />}
    </>
  );
}

function MyWeek() {
  const { data, error } = useGetMyTimetableQuery();
  if (error) return <ErrorNotice error={error} />;
  if (!data) return <div className="skeleton" style={{ width: '60%' }} />;
  if (data.lessons.length === 0) {
    return <EmptyState title="No lessons scheduled for you yet" text="When the school schedules your lessons, your week appears here." />;
  }
  const lessons = indexLessons(data.lessons);
  return (
    <WeekGrid periods={data.periods}
      renderCell={(day, p) => {
        const l = lessons[slotKey(day, p.id)];
        return l ? <><strong>{l.subjectName}</strong><small>{l.className} · {l.sectionName}</small>{l.room && <small>Room {l.room}</small>}</> : null;
      }} />
  );
}

export default function TimetablePage() {
  const isAdmin = useAuth().roles.includes('TENANT_ADMIN');
  const { options, ready } = useSectionOptions();
  const [mode, setMode] = useState(isAdmin ? 'section' : 'mine');
  const [chosen, setChosen] = useState('');
  const sectionId = chosen || options[0]?.id || '';

  return (
    <>
      <PageHeader title="Timetable" subtitle={isAdmin ? 'Build each class’s week. Clashes are blocked automatically.' : 'Your lessons and your classes’ timetables.'}
        action={isAdmin && <Link className="btn btn-secondary" to="/timetable/periods">Set up periods</Link>} />

      {!isAdmin && (
        <div className="summary-chips" role="group" aria-label="View">
          <button className={`btn ${mode === 'mine' ? 'btn-primary' : 'btn-secondary'}`} aria-pressed={mode === 'mine'} onClick={() => setMode('mine')}>My week</button>
          <button className={`btn ${mode === 'section' ? 'btn-primary' : 'btn-secondary'}`} aria-pressed={mode === 'section'} onClick={() => setMode('section')}>A class</button>
        </div>
      )}

      <section className="card">
        {mode === 'mine' ? <MyWeek /> : options.length === 0 ? (
          <EmptyState title={ready && !isAdmin ? 'You are not assigned to any class yet' : 'No classes yet'}
            text={ready && !isAdmin ? 'Ask a school admin to assign you to a class.' : 'Create classes and sections in School setup first.'} />
        ) : (
          <>
            <div className="toolbar">
              <Field label="Class and section">
                <select value={sectionId} onChange={(e) => setChosen(e.target.value)}>
                  {options.map((o) => <option key={o.id} value={o.id}>{o.label}</option>)}
                </select>
              </Field>
            </div>
            <SectionView key={sectionId} sectionId={sectionId} editable={isAdmin} />
          </>
        )}
      </section>
    </>
  );
}