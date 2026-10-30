import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { useAuth } from '../features/auth/useAuth';
import { AUDIENCE_LABEL } from '../app/parent';
import { useArchiveNoticeMutation, useCreateNoticeMutation, useListNoticesQuery } from '../services/noticesApi';
import { Chip, EmptyState, ErrorNotice, Field, PageHeader } from '../components/ui';

const EMPTY = { title: '', body: '', audience: 'ALL' };

export default function NoticesPage() {
  const dispatch = useDispatch();
  const canPublish = useAuth().roles.includes('TENANT_ADMIN');
  const { data: notices, isLoading, error } = useListNoticesQuery();
  const [createNotice, { isLoading: publishing, error: publishError }] = useCreateNoticeMutation();
  const [archive, { isLoading: archiving }] = useArchiveNoticeMutation();
  const [form, setForm] = useState(EMPTY);
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      await createNotice(form).unwrap();
      setForm(EMPTY);
      dispatch(pushToast({ text: 'Notice published' }));
    } catch { /* shown below */ }
  };

  const onArchive = async (notice) => {
    if (!window.confirm(`Remove "${notice.title}" from the notice board?`)) return;
    try {
      await archive(notice.id).unwrap();
      dispatch(pushToast({ text: 'Notice removed' }));
    } catch (err) {
      dispatch(pushToast({ tone: 'error', text: err.message }));
    }
  };

  return (
    <>
      <PageHeader title="Notices" subtitle="Announcements from the school." />

      {canPublish && (
        <section className="card">
          <h2>Publish a notice</h2>
          <form onSubmit={onSubmit} style={{ display: 'grid', gap: 16 }}>
            <Field label="Title"><input name="title" value={form.title} onChange={onChange} required maxLength={200} /></Field>
            <Field label="Message">
              <textarea name="body" value={form.body} onChange={onChange} required maxLength={4000} rows={4}
                        style={{ padding: 12, border: '1px solid var(--border)', borderRadius: 10, background: 'var(--surface)', color: 'var(--text)', font: 'inherit' }} />
            </Field>
            <div className="toolbar" style={{ marginBottom: 0 }}>
              <Field label="Who should see it?">
                <select name="audience" value={form.audience} onChange={onChange}>
                  <option value="ALL">Everyone</option>
                  <option value="PARENTS">Parents only</option>
                  <option value="STAFF">Staff only</option>
                </select>
              </Field>
              <button className="btn btn-primary" disabled={publishing}>{publishing ? 'Publishing…' : 'Publish'}</button>
            </div>
          </form>
          <ErrorNotice error={publishError} />
        </section>
      )}

      <section className="card">
        <h2>Notice board</h2>
        <ErrorNotice error={error} />
        {isLoading && <div className="skeleton" style={{ width: '50%' }} />}
        {notices?.length === 0 && <EmptyState title="No notices yet" text={canPublish ? 'Publish the first one above.' : 'Nothing has been announced yet.'} />}
        {notices?.map((n) => (
          <article className="notice" key={n.id}>
            <h3>{n.title}</h3>
            <div className="notice-meta muted">
              <span>{new Date(n.publishedAt).toLocaleDateString()}</span>
              {canPublish && <Chip tone="info">{AUDIENCE_LABEL[n.audience]}</Chip>}
              {canPublish && <button className="btn-link" disabled={archiving} onClick={() => onArchive(n)}>Remove</button>}
            </div>
            <p>{n.body}</p>
          </article>
        ))}
      </section>
    </>
  );
}