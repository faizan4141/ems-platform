import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../features/auth/useAuth';
import { useGetChildTodayQuery, useListChildrenQuery } from '../services/parentApi';
import { useListNoticesQuery } from '../services/noticesApi';
import { STATUS_LABEL, STATUS_TONE } from '../app/attendance';
import { percentTone, shortDate, todayView } from '../app/parent';
import { Chip, EmptyState, ErrorNotice, PageHeader } from '../components/ui';
import { useGetChildFeesQuery } from '../services/feesApi';
import { formatMoney } from '../app/fees';
import { ChargesTable, PaymentsList } from '../components/LedgerTables';
import Receipt from '../components/Receipt';

function FeesCard({ studentId }) {
  const { data: ledger, error } = useGetChildFeesQuery(studentId);
  const [receipt, setReceipt] = useState(null);

  if (error) return <ErrorNotice error={error} />;
  if (!ledger || ledger.charges.length === 0) return null;   // the school has not billed anything yet

  return (
    <section className="card">
      <div className="page-header" style={{ marginBottom: 12 }}>
        <h2 style={{ margin: 0 }}>Fees</h2>
        <Chip tone={ledger.balance > 0 ? 'warn' : 'ok'}>
          {ledger.balance > 0 ? `${formatMoney(ledger.balance, ledger.currency)} to pay` : 'All paid'}
        </Chip>
      </div>
      <ChargesTable ledger={ledger} />
      <h2 style={{ marginTop: 20 }}>Payments</h2>
      <PaymentsList ledger={ledger} onReceipt={setReceipt} />
      <p className="muted" style={{ marginBottom: 0 }}>To pay a fee, please contact the school office. Online payment is coming soon.</p>
      <Receipt ledger={ledger} payment={receipt} onClose={() => setReceipt(null)} />
    </section>
  );
}

function ChildToday({ studentId }) {
  const { data, error } = useGetChildTodayQuery(studentId);
  if (error) return <ErrorNotice error={error} />;
  if (!data) return <div className="skeleton" style={{ width: '50%' }} />;

  const view = todayView(data.today);
  const placement = data.className && data.sectionName ? `${data.className} · ${data.sectionName}` : 'Class not assigned yet';

  return (
    <>
      <div className="kpis">
        <section className="card">
          <div className="muted">Today · {data.name}</div>
          <div className="today-hero">
            <Chip tone={view.tone}>{view.label}</Chip>
            <span className="muted">{placement}</span>
          </div>
        </section>
        <section className="card">
          <div className="muted">Attendance, last 30 days</div>
          <div className="kpi-value">{data.percentLast30 == null ? '—' : `${data.percentLast30}%`}</div>
          <Chip tone={percentTone(data.percentLast30)}>
            {data.percentLast30 == null ? 'No records yet' : data.percentLast30 >= 90 ? 'Great' : data.percentLast30 >= 75 ? 'Could be better' : 'Needs attention'}
          </Chip>
        </section>
      </div>

      <section className="card">
        <h2>Recent days</h2>
        {data.recent.length === 0 ? (
          <EmptyState title="Nothing recorded yet" text="Attendance appears here once the teacher saves it." />
        ) : (
          <ul className="checklist">
            {data.recent.map((d) => (
              <li key={d.date}>
                <span>{shortDate(d.date)}</span>
                <Chip tone={STATUS_TONE[d.status]}>{STATUS_LABEL[d.status]}</Chip>
              </li>
            ))}
          </ul>
        )}
      </section>
      <FeesCard studentId={studentId} />
    </>
  );
}

export default function ParentHomePage() {
  const { firstName } = useAuth();
  const { data: children, isLoading, error } = useListChildrenQuery();
  const { data: notices = [] } = useListNoticesQuery();
  const [chosen, setChosen] = useState('');
  const childId = chosen || children?.[0]?.studentId;

  return (
    <>
      <PageHeader title={`Hello, ${firstName || 'there'}`} subtitle="Here is how your child is doing at school." />
      <ErrorNotice error={error} />
      {isLoading && <div className="skeleton" style={{ width: '40%' }} />}

      {children?.length === 0 && (
        <section className="card">
          <EmptyState title="No children linked to your account yet"
            text="Please ask the school to check the email address they have on file for you." />
        </section>
      )}

      {children?.length > 1 && (
        <div className="summary-chips" role="group" aria-label="Choose a child">
          {children.map((c) => (
            <button key={c.studentId} aria-pressed={c.studentId === childId}
                    className={`btn ${c.studentId === childId ? 'btn-primary' : 'btn-secondary'}`}
                    onClick={() => setChosen(c.studentId)}>
              {c.name}
            </button>
          ))}
        </div>
      )}

      {childId && <ChildToday key={childId} studentId={childId} />}

      <section className="card">
        <div className="page-header" style={{ marginBottom: 0 }}>
          <h2>Latest notices</h2>
          <Link className="btn-link" to="/notices">See all</Link>
        </div>
        {notices.length === 0 ? <p className="muted">No notices at the moment.</p> : notices.slice(0, 3).map((n) => (
          <div className="notice" key={n.id}>
            <h3>{n.title}</h3>
            <p className="muted" style={{ margin: 0 }}>{n.body.length > 140 ? `${n.body.slice(0, 140)}…` : n.body}</p>
          </div>
        ))}
      </section>
    </>
  );
}