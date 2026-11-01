import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { useAuth } from '../features/auth/useAuth';
import { useListClassesQuery } from '../services/academicApi';
import { useCreateExamMutation, useListExamsQuery, useListSubjectsQuery } from '../services/examsApi';
import { Chip, EmptyState, ErrorNotice, Field, PageHeader } from '../components/ui';

function NewExamForm({ classId, onDone }) {
  const dispatch = useDispatch();
  const { data: subjects = [] } = useListSubjectsQuery();
  const [create, { isLoading, error }] = useCreateExamMutation();
  const [name, setName] = useState('');
  const [rows, setRows] = useState({});   // subjectId -> { on, max, pass }

  const row = (id) => rows[id] ?? { on: false, max: '100', pass: '35' };
  const update = (id, patch) => setRows({ ...rows, [id]: { ...row(id), ...patch } });
  const chosen = subjects.filter((s) => row(s.id).on);

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      await create({
        classId, name,
        subjects: chosen.map((s) => ({ subjectId: s.id, maxMarks: row(s.id).max, passMarks: row(s.id).pass })),
      }).unwrap();
      dispatch(pushToast({ text: 'Exam created' }));
      onDone();
    } catch { /* shown below */ }
  };

  if (subjects.length === 0) {
    return <EmptyState title="Add subjects first" text="Exams are made of subjects. Add them in Subjects and grading." />;
  }
  return (
    <form onSubmit={onSubmit} style={{ display: 'grid', gap: 16 }}>
      <Field label="Exam name"><input value={name} onChange={(e) => setName(e.target.value)} placeholder="Term 1" required maxLength={100} /></Field>
      <div className="table-wrap">
        <table className="table">
          <thead><tr><th></th><th>Subject</th><th>Max marks</th><th>Pass marks</th></tr></thead>
          <tbody>
            {subjects.map((s) => (
              <tr key={s.id}>
                <td><input type="checkbox" aria-label={`Include ${s.name}`} checked={row(s.id).on} onChange={(e) => update(s.id, { on: e.target.checked })} /></td>
                <td>{s.name}</td>
                <td><input className="marks-input" inputMode="decimal" value={row(s.id).max} disabled={!row(s.id).on} onChange={(e) => update(s.id, { max: e.target.value })} /></td>
                <td><input className="marks-input" inputMode="decimal" value={row(s.id).pass} disabled={!row(s.id).on} onChange={(e) => update(s.id, { pass: e.target.value })} /></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="row-actions">
        <button className="btn btn-primary" disabled={isLoading || chosen.length === 0}>{isLoading ? 'Creating…' : 'Create exam'}</button>
        <button type="button" className="btn btn-secondary" onClick={onDone}>Cancel</button>
      </div>
      <ErrorNotice error={error} />
    </form>
  );
}

export default function ExamsPage() {
  const isAdmin = useAuth().roles.includes('TENANT_ADMIN');
  const { data: classes = [] } = useListClassesQuery();
  const [chosen, setChosen] = useState('');
  const [showNew, setShowNew] = useState(false);
  const classId = chosen || classes[0]?.id || '';
  const { data: exams = [], error } = useListExamsQuery(classId, { skip: !classId });

  return (
    <>
      <PageHeader title="Exams" subtitle="Enter marks, publish results and print report cards."
        action={isAdmin && (
          <div className="row-actions">
            <Link className="btn btn-secondary" to="/exams/setup">Subjects and grading</Link>
            <button className="btn btn-primary" disabled={!classId} onClick={() => setShowNew((v) => !v)}>
              {showNew ? 'Close form' : 'New exam'}
            </button>
          </div>
        )} />

      {classes.length === 0 ? (
        <section className="card"><EmptyState title="No classes yet" text="Create classes in School setup first." /></section>
      ) : (
        <>
          <section className="card">
            <Field label="Class">
              <select value={classId} onChange={(e) => { setChosen(e.target.value); setShowNew(false); }}>
                {classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </Field>
          </section>

          {isAdmin && showNew && <section className="card"><h2>New exam</h2><NewExamForm classId={classId} onDone={() => setShowNew(false)} /></section>}

          <section className="card">
            <h2>Exams for this class</h2>
            <ErrorNotice error={error} />
            {exams.length === 0 ? <EmptyState title="No exams yet" text={isAdmin ? 'Create the first one above.' : 'Nothing has been set up yet.'} /> : (
              <ul className="checklist">
                {exams.map((e) => (
                  <li key={e.id}>
                    <span>
                      <strong>{e.name}</strong> <Chip tone={e.status === 'PUBLISHED' ? 'ok' : 'warn'}>{e.status === 'PUBLISHED' ? 'Published' : 'Draft'}</Chip><br />
                      <span className="muted">{e.subjects.map((s) => s.subjectName).join(', ')}</span>
                    </span>
                    <Link className="btn btn-secondary" to={`/exams/${e.id}`}>Open</Link>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </>
  );
}