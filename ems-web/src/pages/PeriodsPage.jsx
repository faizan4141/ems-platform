import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { hhmm, timeRange, validatePeriod } from '../app/timetable';
import {
  useCreatePeriodMutation, useDeletePeriodMutation, useListPeriodsQuery, useUpdatePeriodMutation,
} from '../services/timetableApi';
import { Chip, EmptyState, ErrorNotice, Field, PageHeader } from '../components/ui';

const EMPTY = { number: '', name: '', startTime: '', endTime: '', breakPeriod: false };

export default function PeriodsPage() {
  const dispatch = useDispatch();
  const { data: periods = [], error } = useListPeriodsQuery();
  const [createPeriod, createState] = useCreatePeriodMutation();
  const [updatePeriod, updateState] = useUpdatePeriodMutation();
  const [deletePeriod] = useDeletePeriodMutation();
  const [form, setForm] = useState(EMPTY);
  const [editingId, setEditingId] = useState(null);

  const problem = form.number === '' && !form.name ? null : validatePeriod(form);
  const busy = createState.isLoading || updateState.isLoading;
  const saveError = createState.error || updateState.error;
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  const reset = () => {
    setForm(EMPTY);
    setEditingId(null);
    createState.reset();
    updateState.reset();
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const body = { number: Number(form.number), name: form.name.trim(), startTime: form.startTime, endTime: form.endTime, breakPeriod: form.breakPeriod };
    try {
      if (editingId) await updatePeriod({ id: editingId, ...body }).unwrap();
      else await createPeriod(body).unwrap();
      dispatch(pushToast({ text: editingId ? 'Period updated' : 'Period added' }));
      reset();
    } catch { /* shown below */ }
  };

  const edit = (p) => {
    setEditingId(p.id);
    setForm({ number: String(p.number), name: p.name, startTime: hhmm(p.startTime), endTime: hhmm(p.endTime), breakPeriod: p.breakPeriod });
  };

  const remove = async (p) => {
    try {
      await deletePeriod(p.id).unwrap();
    } catch (err) {
      dispatch(pushToast({ tone: 'error', text: err.message }));
    }
  };

  return (
    <>
      <Link to="/timetable" className="btn-link">← Timetable</Link>
      <div style={{ height: 12 }} />
      <PageHeader title="Periods" subtitle="Set up the school day once. Every class timetable uses these periods." />

      <section className="card">
        <h2>{editingId ? 'Edit period' : 'Add a period'}</h2>
        <form onSubmit={onSubmit} className="form-grid">
          <Field label="Number"><input name="number" value={form.number} onChange={onChange} inputMode="numeric" required /></Field>
          <Field label="Name"><input name="name" value={form.name} onChange={onChange} placeholder="Period 1, Lunch" required maxLength={30} /></Field>
          <Field label="Starts"><input type="time" name="startTime" value={form.startTime} onChange={onChange} required /></Field>
          <Field label="Ends"><input type="time" name="endTime" value={form.endTime} onChange={onChange} required /></Field>
          <label className="field" style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
            <input type="checkbox" name="breakPeriod" checked={form.breakPeriod} onChange={onChange} /> This is a break
          </label>
          <div className="row-actions">
            <button className="btn btn-primary" disabled={busy || !!problem || form.number === ''}>{busy ? 'Saving…' : editingId ? 'Save' : 'Add period'}</button>
            {editingId && <button type="button" className="btn btn-secondary" onClick={reset}>Cancel</button>}
          </div>
        </form>
        {problem && <div className="notice-error">{problem}</div>}
        <ErrorNotice error={saveError} />
      </section>

      <section className="card">
        <h2>The school day</h2>
        <ErrorNotice error={error} />
        {periods.length === 0 ? <EmptyState title="No periods yet" text="Add the first one above, for example Period 1, 08:00 to 08:40." /> : (
          <ul className="checklist">
            {periods.map((p) => (
              <li key={p.id}>
                <span><strong>{p.number}. {p.name}</strong> <span className="muted">· {timeRange(p)}</span> {p.breakPeriod && <Chip tone="neutral">Break</Chip>}</span>
                <span className="row-actions">
                  <button className="btn btn-secondary" onClick={() => edit(p)}>Edit</button>
                  <button className="btn btn-secondary" onClick={() => remove(p)}>Delete</button>
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}