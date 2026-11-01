import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { formatMoney } from '../app/fees';
import { shortDate } from '../app/parent';
import { useListClassesQuery } from '../services/academicApi';
import {
  useCreateFeeItemMutation, useDeleteFeeItemMutation, useGenerateChargesMutation, useGetFeeSummaryQuery,
  useListFeeItemsQuery,
} from '../services/feesApi';
import { EmptyState, ErrorNotice, Field, PageHeader } from '../components/ui';

const EMPTY = { name: '', amount: '', dueDate: '' };

export default function FeeSetupPage() {
  const dispatch = useDispatch();
  const { data: classes = [] } = useListClassesQuery();
  const { data: summary } = useGetFeeSummaryQuery();
  const [chosen, setChosen] = useState('');
  const [form, setForm] = useState(EMPTY);
  const classId = chosen || classes[0]?.id || '';

  const { data: items = [], error } = useListFeeItemsQuery(classId, { skip: !classId });
  const [createItem, { isLoading: creating, error: createError }] = useCreateFeeItemMutation();
  const [deleteItem] = useDeleteFeeItemMutation();
  const [generate, { isLoading: generating }] = useGenerateChargesMutation();
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      await createItem({ classId, ...form }).unwrap();
      setForm(EMPTY);
      dispatch(pushToast({ text: 'Fee added' }));
    } catch { /* shown below */ }
  };

  const onDelete = async (item) => {
    if (!window.confirm(`Delete "${item.name}"?`)) return;
    try {
      await deleteItem(item.id).unwrap();
    } catch (err) {
      dispatch(pushToast({ tone: 'error', text: err.message }));
    }
  };

  const onGenerate = async () => {
    try {
      const r = await generate(classId).unwrap();
      dispatch(pushToast({ text: r.created === 0 ? 'Everything was already generated' : `${r.created} charges created for ${r.students} students` }));
    } catch (err) {
      dispatch(pushToast({ tone: 'error', text: err.message }));
    }
  };

  return (
    <>
      <Link to="/fees" className="btn-link">← Fees</Link>
      <div style={{ height: 12 }} />
      <PageHeader title="Fee setup" subtitle="Define what each class pays, then generate the charges for its students." />

      {classes.length === 0 ? (
        <section className="card"><EmptyState title="No classes yet" text="Create classes in School setup first." /></section>
      ) : (
        <>
          <section className="card">
            <Field label="Class">
              <select value={classId} onChange={(e) => setChosen(e.target.value)}>
                {classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </Field>
          </section>

          <section className="card">
            <h2>Add a fee</h2>
            <form onSubmit={onSubmit} className="form-grid">
              <Field label="Name"><input name="name" value={form.name} onChange={onChange} placeholder="Tuition, Term 1" required maxLength={100} /></Field>
              <Field label="Amount"><input name="amount" value={form.amount} onChange={onChange} inputMode="decimal" pattern="\d+(\.\d{1,2})?" title="A number with at most two decimals" required /></Field>
              <Field label="Due date"><input type="date" name="dueDate" value={form.dueDate} onChange={onChange} required /></Field>
              <button className="btn btn-primary" disabled={creating}>{creating ? 'Adding…' : 'Add fee'}</button>
            </form>
            <ErrorNotice error={createError} />
          </section>

          <section className="card">
            <div className="page-header" style={{ marginBottom: 12 }}>
              <h2 style={{ margin: 0 }}>Fees for this class</h2>
              <button className="btn btn-primary" disabled={generating || items.length === 0} onClick={onGenerate}>
                {generating ? 'Generating…' : 'Generate for all students'}
              </button>
            </div>
            <ErrorNotice error={error} />
            {items.length === 0 ? <EmptyState title="No fees yet" text="Add the first one above." /> : (
              <ul className="checklist">
                {items.map((i) => (
                  <li key={i.id}>
                    <span>
                      <strong>{i.name}</strong> <span className="muted">· {formatMoney(i.amount, summary?.currency)} · due {shortDate(i.dueDate)}</span><br />
                      <span className="muted">{i.chargesGenerated === 0 ? 'Not generated yet' : `Generated for ${i.chargesGenerated} students`}</span>
                    </span>
                    {i.chargesGenerated === 0 && <button className="btn btn-secondary" onClick={() => onDelete(i)}>Delete</button>}
                  </li>
                ))}
              </ul>
            )}
            <p className="muted" style={{ marginBottom: 0 }}>
              Generating is safe to repeat: it only creates charges that are missing, for example for students who joined later.
            </p>
          </section>
        </>
      )}
    </>
  );
}