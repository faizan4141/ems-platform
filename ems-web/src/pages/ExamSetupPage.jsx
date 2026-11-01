import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { validateScale } from '../app/exams';
import {
  useCreateSubjectMutation, useDeleteSubjectMutation, useGetScaleQuery, useListSubjectsQuery, useSaveScaleMutation,
} from '../services/examsApi';
import { EmptyState, ErrorNotice, Field, PageHeader } from '../components/ui';

function SubjectsCard() {
  const dispatch = useDispatch();
  const { data: subjects = [], error } = useListSubjectsQuery();
  const [createSubject, { isLoading, error: createError }] = useCreateSubjectMutation();
  const [deleteSubject] = useDeleteSubjectMutation();
  const [name, setName] = useState('');

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      await createSubject({ name }).unwrap();
      setName('');
    } catch { /* shown below */ }
  };

  const onDelete = async (subject) => {
    try {
      await deleteSubject(subject.id).unwrap();
    } catch (err) {
      dispatch(pushToast({ tone: 'error', text: err.message }));
    }
  };

  return (
    <section className="card">
      <h2>Subjects</h2>
      <form onSubmit={onSubmit} className="form-grid">
        <Field label="New subject"><input value={name} onChange={(e) => setName(e.target.value)} placeholder="Mathematics" required maxLength={100} /></Field>
        <button className="btn btn-primary" disabled={isLoading}>Add subject</button>
      </form>
      <ErrorNotice error={createError || error} />
      {subjects.length === 0 ? <EmptyState title="No subjects yet" text="Add the subjects your school teaches." /> : (
        <ul className="checklist" style={{ marginTop: 12 }}>
          {subjects.map((s) => (
            <li key={s.id}><strong>{s.name}</strong><button className="btn btn-secondary" onClick={() => onDelete(s)}>Delete</button></li>
          ))}
        </ul>
      )}
    </section>
  );
}

function ScaleEditor({ initial }) {
  const dispatch = useDispatch();
  const [save, { isLoading, error }] = useSaveScaleMutation();
  const [rows, setRows] = useState(() => initial.map((b) => ({ min: String(b.minPercent), grade: b.grade, label: b.label ?? '' })));
  const problem = validateScale(rows);
  const update = (i, patch) => setRows(rows.map((r, idx) => (idx === i ? { ...r, ...patch } : r)));

  const onSave = async () => {
    try {
      await save(rows.map((r) => ({ minPercent: r.min, grade: r.grade.trim(), label: r.label.trim() || null }))).unwrap();
      dispatch(pushToast({ text: 'Grading scale saved' }));
    } catch { /* shown below */ }
  };

  return (
    <>
      <div className="table-wrap">
        <table className="table">
          <thead><tr><th>From (%)</th><th>Grade</th><th>Description</th><th></th></tr></thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={i}>
                <td><input className="marks-input" inputMode="decimal" aria-label="Starting percentage" value={r.min} onChange={(e) => update(i, { min: e.target.value })} /></td>
                <td><input className="marks-input" aria-label="Grade" value={r.grade} maxLength={10} onChange={(e) => update(i, { grade: e.target.value })} /></td>
                <td><input aria-label="Description" value={r.label} maxLength={50} onChange={(e) => update(i, { label: e.target.value })} /></td>
                <td><button className="btn btn-secondary" onClick={() => setRows(rows.filter((_, idx) => idx !== i))}>Remove</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="row-actions" style={{ marginTop: 12 }}>
        <button className="btn btn-secondary" onClick={() => setRows([...rows, { min: '', grade: '', label: '' }])}>Add a grade</button>
        <button className="btn btn-primary" disabled={isLoading || !!problem} onClick={onSave}>{isLoading ? 'Saving…' : 'Save scale'}</button>
      </div>
      {problem && <div className="notice-error">{problem}</div>}
      <ErrorNotice error={error} />
    </>
  );
}

function ScaleCard() {
  const { data } = useGetScaleQuery();
  return (
    <section className="card">
      <h2>Grading scale</h2>
      <p className="muted" style={{ marginTop: 0 }}>A student gets the highest grade whose starting percentage they reach. The lowest grade must start at 0.</p>
      {data ? <ScaleEditor initial={data} /> : <div className="skeleton" style={{ width: '50%' }} />}
    </section>
  );
}

export default function ExamSetupPage() {
  return (
    <>
      <Link to="/exams" className="btn-link">← Exams</Link>
      <div style={{ height: 12 }} />
      <PageHeader title="Subjects and grading" subtitle="Set these up once. Exams build on them." />
      <SubjectsCard />
      <ScaleCard />
    </>
  );
}