import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { parseSections } from '../app/format';
import {
  useCreateClassMutation, useCreateYearMutation, useGetProfileQuery, useListClassesQuery,
  useListYearsQuery, useSaveProfileMutation, useSetCurrentYearMutation,
} from '../services/academicApi';
import { Chip, EmptyState, ErrorNotice, Field, PageHeader } from '../components/ui';

const BOARDS = ['CBSE', 'ICSE', 'State board', 'IB', 'Other'];
const PROFILE_FIELDS = ['displayName', 'board', 'affiliationNo', 'addressLine', 'city', 'state', 'postalCode', 'phone', 'email', 'website'];

function ProfileForm({ initial }) {
  const dispatch = useDispatch();
  const [save, { isLoading, error }] = useSaveProfileMutation();
  const [form, setForm] = useState(() => Object.fromEntries(PROFILE_FIELDS.map((k) => [k, initial[k] ?? ''])));
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      await save(form).unwrap();
      dispatch(pushToast({ text: 'School profile saved' }));
    } catch { /* shown below */ }
  };

  const text = (name, label, extra = {}) => (
    <Field label={label}><input name={name} value={form[name]} onChange={onChange} {...extra} /></Field>
  );

  return (
    <form onSubmit={onSubmit} className="form-grid">
      {text('displayName', 'School name', { required: true, maxLength: 200 })}
      <Field label="Board">
        <select name="board" value={form.board} onChange={onChange}>
          <option value="">Select…</option>
          {BOARDS.map((b) => <option key={b} value={b}>{b}</option>)}
        </select>
      </Field>
      {text('affiliationNo', 'Affiliation number')}
      {text('addressLine', 'Address')}
      {text('city', 'City')}
      {text('state', 'State')}
      {text('postalCode', 'Postal code')}
      {text('phone', 'Phone')}
      {text('email', 'Email', { type: 'email' })}
      {text('website', 'Website')}
      <button className="btn btn-primary" disabled={isLoading}>{isLoading ? 'Saving…' : 'Save profile'}</button>
      <div style={{ gridColumn: '1 / -1' }}><ErrorNotice error={error} /></div>
    </form>
  );
}

function ProfileCard() {
  const { data } = useGetProfileQuery();
  return (
    <section className="card">
      <h2>School profile</h2>
      {data ? <ProfileForm initial={data} /> : <div className="skeleton" style={{ width: '60%' }} />}
    </section>
  );
}

function YearsCard() {
  const dispatch = useDispatch();
  const { data: years = [], error } = useListYearsQuery();
  const [createYear, { isLoading, error: createError }] = useCreateYearMutation();
  const [setCurrent, { isLoading: switching }] = useSetCurrentYearMutation();
  const empty = { name: '', startDate: '', endDate: '', makeCurrent: false };
  const [form, setForm] = useState(empty);
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      await createYear(form).unwrap();
      setForm(empty);
      dispatch(pushToast({ text: 'Academic year added' }));
    } catch { /* shown below */ }
  };

  const onSetCurrent = async (year) => {
    try {
      await setCurrent(year.id).unwrap();
      dispatch(pushToast({ text: `${year.name} is now the current year` }));
    } catch (err) {
      dispatch(pushToast({ tone: 'error', text: err.message }));
    }
  };

  return (
    <section className="card">
      <h2>Academic years</h2>
      <form onSubmit={onSubmit} className="form-grid">
        <Field label="Name"><input name="name" value={form.name} onChange={onChange} placeholder="2026-27" required maxLength={50} /></Field>
        <Field label="Starts"><input type="date" name="startDate" value={form.startDate} onChange={onChange} required /></Field>
        <Field label="Ends"><input type="date" name="endDate" value={form.endDate} onChange={onChange} required /></Field>
        <label className="field" style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
          <input type="checkbox" name="makeCurrent" checked={form.makeCurrent} onChange={onChange} /> Make current
        </label>
        <button className="btn btn-primary" disabled={isLoading}>{isLoading ? 'Adding…' : 'Add year'}</button>
      </form>
      <ErrorNotice error={createError || error} />
      {years.length === 0
        ? <EmptyState title="No academic years yet" text="Add your first one above. It becomes the current year." />
        : (
          <ul className="checklist" style={{ marginTop: 12 }}>
            {years.map((y) => (
              <li key={y.id}>
                <span><strong>{y.name}</strong> <span className="muted">{y.startDate} → {y.endDate}</span></span>
                {y.current
                  ? <Chip tone="ok">Current</Chip>
                  : <button className="btn btn-secondary" disabled={switching} onClick={() => onSetCurrent(y)}>Set as current</button>}
              </li>
            ))}
          </ul>
        )}
    </section>
  );
}

function ClassesCard() {
  const dispatch = useDispatch();
  const { data: years = [] } = useListYearsQuery();
  const [selected, setSelected] = useState('');
  const { data: classes = [], error } = useListClassesQuery(selected || undefined);
  const [createClass, { isLoading, error: createError }] = useCreateClassMutation();
  const empty = { name: '', gradeLevel: '', sections: '' };
  const [form, setForm] = useState(empty);
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const yearId = selected || years.find((y) => y.current)?.id;

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      await createClass({
        academicYearId: yearId, name: form.name, gradeLevel: Number(form.gradeLevel),
        sections: parseSections(form.sections),
      }).unwrap();
      setForm(empty);
      dispatch(pushToast({ text: 'Class added' }));
    } catch { /* shown below */ }
  };

  return (
    <section className="card">
      <h2>Classes and sections</h2>
      {years.length === 0 ? (
        <EmptyState title="Add an academic year first" text="Classes belong to an academic year." />
      ) : (
        <>
          <Field label="Academic year">
            <select value={selected} onChange={(e) => setSelected(e.target.value)}>
              <option value="">Current year</option>
              {years.map((y) => <option key={y.id} value={y.id}>{y.name}</option>)}
            </select>
          </Field>
          <form onSubmit={onSubmit} className="form-grid" style={{ marginTop: 16 }}>
            <Field label="Class name"><input name="name" value={form.name} onChange={onChange} placeholder="Class 8" required maxLength={50} /></Field>
            <Field label="Grade (0 = nursery, up to 12)">
              <input type="number" name="gradeLevel" min="0" max="12" value={form.gradeLevel} onChange={onChange} required />
            </Field>
            <Field label="Sections (comma-separated)"><input name="sections" value={form.sections} onChange={onChange} placeholder="A, B, C" /></Field>
            <button className="btn btn-primary" disabled={isLoading || !yearId}>{isLoading ? 'Adding…' : 'Add class'}</button>
          </form>
          <ErrorNotice error={createError || error} />
          {classes.length === 0
            ? <EmptyState title="No classes yet" text="Add Class 1, Class 2 and so on above." />
            : (
              <ul className="checklist" style={{ marginTop: 12 }}>
                {classes.map((c) => (
                  <li key={c.id}>
                    <strong>{c.name}</strong>
                    <span style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                      {c.sections.length === 0 ? <span className="muted">No sections</span>
                        : c.sections.map((s) => <Chip key={s.id} tone="info">{s.name}</Chip>)}
                    </span>
                  </li>
                ))}
              </ul>
            )}
        </>
      )}
    </section>
  );
}

export default function SetupPage() {
  return (
    <>
      <PageHeader title="School setup" subtitle="Describe your school once. Everything else builds on this." />
      <ProfileCard />
      <YearsCard />
      <ClassesCard />
    </>
  );
}