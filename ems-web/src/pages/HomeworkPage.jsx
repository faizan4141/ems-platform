import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { useAuth } from '../features/auth/useAuth';
import { useSectionOptions } from '../app/useSectionOptions';
import { KIND, isoDate, linksFromText, linksToText, postableSubjects, validateHomework, effectiveSection, effectiveSubject } from '../app/homework';
import { shortDate } from '../app/parent';
import { useListSubjectsQuery } from '../services/examsApi';
import { useGetMyTeachingQuery } from '../services/staffApi';
import {
  useCreateHomeworkMutation, useDeleteHomeworkMutation, useListHomeworkQuery, useUpdateHomeworkMutation,
} from '../services/homeworkApi';
import ConfirmDialog from '../components/ConfirmDialog';
import LinkList from '../components/LinkList';
import { Chip, EmptyState, ErrorNotice, Field, PageHeader } from '../components/ui';

function HomeworkForm({ options, isAdmin, teaching, subjects, editing, preset, onDone }) {
  const dispatch = useDispatch();
  const [create, createState] = useCreateHomeworkMutation();
  const [update, updateState] = useUpdateHomeworkMutation();
  const [form, setForm] = useState(() => ({
    sectionId: editing?.sectionId || preset.sectionId || '',
    subjectId: editing?.subjectId || preset.subjectId || '',
    kind: editing?.kind ?? 'HOMEWORK',
    title: editing?.title ?? '',
    description: editing?.description ?? '',
    dueDate: editing?.dueDate ?? '',
    linksText: linksToText(editing?.links),
  }));

  // What is shown is what is used, even before the person touches the dropdowns.
  const sectionId = editing ? form.sectionId : effectiveSection(form.sectionId, options);
  const allowed = editing
    ? [{ id: editing.subjectId, name: editing.subjectName }]
    : postableSubjects({ isAdmin, allSubjects: subjects, teaching, sectionId });
  const subjectId = editing ? form.subjectId : effectiveSubject(form.subjectId, allowed);

  const problem = validateHomework(form);
  const incomplete = !sectionId || !subjectId;
  const busy = createState.isLoading || updateState.isLoading;
  const error = createState.error || updateState.error;

  const onChange = (e) => {
    const next = { ...form, [e.target.name]: e.target.value };
    if (e.target.name === 'sectionId') next.subjectId = '';   // the subjects on offer depend on the section
    setForm(next);
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const body = {
      title: form.title.trim(), description: form.description.trim(),
      dueDate: form.kind === 'HOMEWORK' ? form.dueDate : null, links: linksFromText(form.linksText),
    };
    try {
      if (editing) await update({ id: editing.id, ...body }).unwrap();
      else await create({ sectionId, subjectId, kind: form.kind, ...body }).unwrap();
      dispatch(pushToast({ text: editing ? 'Post updated' : 'Posted. Parents can see it now.' }));
      onDone();
    } catch { /* shown below */ }
  };

  return (
    <form onSubmit={onSubmit} style={{ display: 'grid', gap: 16 }}>
      <div className="form-grid">
        <Field label="Class and section">
          <select name="sectionId" value={sectionId} onChange={onChange} disabled={!!editing} required>
            {editing && <option value={editing.sectionId}>{editing.className} · {editing.sectionName}</option>}
            {!editing && options.map((o) => <option key={o.id} value={o.id}>{o.label}</option>)}
          </select>
        </Field>
        <Field label="Subject">
          <select name="subjectId" value={subjectId} onChange={onChange} disabled={!!editing} required>
            <option value="" disabled>Select…</option>
            {allowed.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </Field>
        <Field label="Type">
          <select name="kind" value={form.kind} onChange={onChange} disabled={!!editing}>
            <option value="HOMEWORK">Homework (has a due date)</option>
            <option value="NOTE">Class note</option>
          </select>
        </Field>
        {form.kind === 'HOMEWORK' && (
          <Field label="Due date">
            <input type="date" name="dueDate" value={form.dueDate} onChange={onChange} min={editing ? undefined : isoDate()} required />
          </Field>
        )}
      </div>
      <Field label="Title"><input name="title" value={form.title} onChange={onChange} maxLength={200} required /></Field>
      <Field label="Instructions or notes">
        <textarea name="description" value={form.description} onChange={onChange} rows={5} maxLength={4000} required />
      </Field>
      <Field label="Links (optional, one per line, up to 5)">
        <textarea name="linksText" value={form.linksText} onChange={onChange} rows={3} placeholder="https://…" />
      </Field>
      <div className="row-actions">
        <button className="btn btn-primary" disabled={busy || incomplete || !!problem}>{busy ? 'Posting…' : editing ? 'Save changes' : 'Post'}</button>
        <button type="button" className="btn btn-secondary" onClick={onDone}>Cancel</button>
      </div>
      {(form.title || form.description || form.linksText) && problem && <div className="notice-error">{problem}</div>}
      {allowed.length === 0 && !editing && <div className="notice-error">No subject is assigned to you in this section. Ask a school admin to assign you.</div>}
      <ErrorNotice error={error} />
    </form>
  );
}

export default function HomeworkPage() {
  const dispatch = useDispatch();
  const isAdmin = useAuth().roles.includes('TENANT_ADMIN');
  const [params] = useSearchParams();
  const preset = { sectionId: params.get('section') ?? '', subjectId: params.get('subject') ?? '' };
  const { options, ready } = useSectionOptions();
  const { data: teaching } = useGetMyTeachingQuery(undefined, { skip: isAdmin });
  const { data: subjects = [] } = useListSubjectsQuery();
  const [filterSection, setFilterSection] = useState('');
  const [filterKind, setFilterKind] = useState('');
  const [formOpen, setFormOpen] = useState(!!preset.sectionId);
  const [editing, setEditing] = useState(null);
  const [removing, setRemoving] = useState(null);
  const { data: items, isLoading, error } = useListHomeworkQuery({ sectionId: filterSection, kind: filterKind }, { skip: !ready });
  const [remove, removeState] = useDeleteHomeworkMutation();

  if (!isAdmin && ready && options.length === 0) {
    return (
      <>
        <PageHeader title="Homework" />
        <section className="card">
          <EmptyState title="You are not assigned to any class yet" text="Ask a school admin to assign you. You can then post homework for your classes." />
        </section>
      </>
    );
  }

  const showForm = formOpen || !!editing;
  const closeForm = () => { setFormOpen(false); setEditing(null); };

  const confirmRemove = async () => {
    try {
      await remove(removing.id).unwrap();
      dispatch(pushToast({ text: 'Post removed' }));
      setRemoving(null);
    } catch { /* shown inside the dialog */ }
  };

  return (
    <>
      <PageHeader title="Homework" subtitle={isAdmin ? 'Everything set across the school.' : 'Post homework and notes for your classes.'}
        action={<button className="btn btn-primary" disabled={!ready || options.length === 0}
                        onClick={() => { setEditing(null); setFormOpen((v) => !v); }}>{showForm && !editing ? 'Close' : 'New post'}</button>} />

      {showForm && (
        <section className="card">
          <h2>{editing ? 'Edit post' : 'New post'}</h2>
          <HomeworkForm key={editing?.id ?? 'new'} options={options} isAdmin={isAdmin} teaching={teaching} subjects={subjects}
                        editing={editing} preset={preset} onDone={closeForm} />
        </section>
      )}

      <section className="card">
        <div className="toolbar">
          <Field label="Class and section">
            <select value={filterSection} onChange={(e) => setFilterSection(e.target.value)}>
              <option value="">{isAdmin ? 'All classes' : 'All my classes'}</option>
              {options.map((o) => <option key={o.id} value={o.id}>{o.label}</option>)}
            </select>
          </Field>
          <Field label="Show">
            <select value={filterKind} onChange={(e) => setFilterKind(e.target.value)}>
              <option value="">Homework and notes</option>
              <option value="HOMEWORK">Homework only</option>
              <option value="NOTE">Class notes only</option>
            </select>
          </Field>
        </div>
        <ErrorNotice error={error} />
        {isLoading && <div className="skeleton" style={{ width: '50%' }} />}
        {items?.length === 0 && <EmptyState title="Nothing posted yet" text="Use “New post” to share homework or a class note." />}
        {items?.map((h) => (
          <article className="hw-item" key={h.id}>
            <div className="notice-meta">
              <Chip tone={KIND[h.kind].tone}>{KIND[h.kind].label}</Chip>
              <strong>{h.subjectName}</strong>
              <span className="muted">· {h.className} · {h.sectionName}</span>
              {h.dueDate && <Chip tone="warn">Due {shortDate(h.dueDate)}</Chip>}
            </div>
            <h3>{h.title}</h3>
            <p>{h.description}</p>
            <LinkList links={h.links} />
            <div className="notice-meta muted">
              <span>Posted by {h.authorName} · {new Date(h.createdAt).toLocaleDateString()}</span>
              {h.canEdit && (
                <>
                  <button className="btn-link" onClick={() => { setEditing(h); window.scrollTo({ top: 0, behavior: 'smooth' }); }}>Edit</button>
                  <button className="btn-link" onClick={() => setRemoving(h)}>Remove</button>
                </>
              )}
            </div>
          </article>
        ))}
      </section>

      <ConfirmDialog open={!!removing} title="Remove this post?" text="Parents will no longer see it." confirmLabel="Remove"
        busy={removeState.isLoading} error={removeState.error} onConfirm={confirmRemove}
        onClose={() => { setRemoving(null); removeState.reset(); }} />
    </>
  );
}