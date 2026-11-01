import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { formatMoney } from '../app/fees';
import { shortDate } from '../app/parent';
import { useListClassesQuery } from '../services/academicApi';
import { useGetDuesQuery, useGetFeeSummaryQuery, useRunRemindersMutation } from '../services/feesApi';
import { Chip, EmptyState, ErrorNotice, Field, PageHeader, SkeletonRows } from '../components/ui';

export default function FeesPage() {
  const dispatch = useDispatch();
  const [classId, setClassId] = useState('');
  const { data: classes = [] } = useListClassesQuery();
  const { data: summary, error: summaryError } = useGetFeeSummaryQuery();
  const { data: dues, isLoading, error } = useGetDuesQuery(classId);
  const [runReminders, { isLoading: reminding }] = useRunRemindersMutation();
  const money = (v) => formatMoney(v, summary?.currency);

  const onRemind = async () => {
    if (!window.confirm('Send reminders now to families with fees due soon or overdue?')) return;
    try {
      const result = await runReminders().unwrap();
      dispatch(pushToast({ text: `${result.reminded} reminder${result.reminded === 1 ? '' : 's'} sent` }));
    } catch (err) {
      dispatch(pushToast({ tone: 'error', text: err.message }));
    }
  };

  const kpis = summary ? [
    { label: 'Billed', value: money(summary.billed), chip: <Chip tone="info">All fees</Chip> },
    { label: 'Collected', value: money(summary.collected), chip: <Chip tone="ok">Received</Chip> },
    { label: 'Outstanding', value: money(summary.outstanding), chip: <Chip tone="warn">Still to collect</Chip> },
    { label: 'Overdue', value: money(summary.overdue), chip: <Chip tone={summary.studentsWithOverdue ? 'bad' : 'ok'}>{summary.studentsWithOverdue} students</Chip> },
  ] : [];

  return (
    <>
      <PageHeader title="Fees" subtitle="What has been billed, collected and is still due."
        action={(
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-secondary" disabled={reminding} onClick={onRemind}>{reminding ? 'Sending…' : 'Send reminders'}</button>
            <Link className="btn btn-secondary" to="/fees/online">Online payments</Link>
            <Link className="btn btn-primary" to="/fees/setup">Fee setup</Link>
          </div>
        )} />
      <ErrorNotice error={summaryError} />
      <div className="kpis">
        {kpis.map((k) => (
          <div className="card" key={k.label}>
            <div className="muted">{k.label}</div>
            <div className="kpi-value">{k.value}</div>
            {k.chip}
          </div>
        ))}
      </div>

      <section className="card">
        <div className="toolbar">
          <h2 style={{ margin: 0, flex: 1 }}>Who owes</h2>
          <Field label="Class">
            <select value={classId} onChange={(e) => setClassId(e.target.value)}>
              <option value="">All classes</option>
              {classes.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </Field>
        </div>
        <ErrorNotice error={error} />
        {dues?.rows.length === 0 && <EmptyState title="Nothing outstanding" text="Either everyone has paid, or no fees have been generated yet. Start from Fee setup." />}
        {(isLoading || dues?.rows.length > 0) && (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Student</th><th>Class</th><th>Balance</th><th>Overdue</th><th>Earliest due</th></tr></thead>
              <tbody>
                {isLoading && <SkeletonRows cols={5} />}
                {dues?.rows.map((r) => (
                  <tr key={r.studentId}>
                    <td>
                      <Link to={`/fees/students/${r.studentId}`}><strong>{r.studentName}</strong></Link>
                      <div className="muted">{r.admissionNo}</div>
                    </td>
                    <td className="muted">{r.className && r.sectionName ? `${r.className} · ${r.sectionName}` : '—'}</td>
                    <td>{formatMoney(r.balance, dues.currency)}</td>
                    <td>{r.overdue > 0 ? <Chip tone="bad">{formatMoney(r.overdue, dues.currency)}</Chip> : '—'}</td>
                    <td className="muted">{shortDate(r.earliestDue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}