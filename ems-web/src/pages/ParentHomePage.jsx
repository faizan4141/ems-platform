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
import { useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { api } from '../services/api';
import { useGetAttemptQuery, useStartPaymentMutation } from '../services/feesApi';
import { newKey } from '../app/fees';
import { payableCharges, returnMessage, safeRedirectUrl, selectedTotal } from '../app/onlinePay';
import { useListChildResultsQuery } from '../services/examsApi';
import { RESULT, fmt } from '../app/exams';
import ReportCardDialog from '../components/ReportCardDialog';

function PaymentBanner() {
  const dispatch = useDispatch();
  const [params, setParams] = useSearchParams();
  const id = params.get('payment');
  const { data, refetch } = useGetAttemptQuery(id, { skip: !id });
  const status = data?.status;

  // Poll only while the result is unknown (a real provider may redirect back before its webhook arrives).
  useEffect(() => {
    if (!id || (status && status !== 'PENDING')) return undefined;
    const timer = setInterval(refetch, 3000);
    return () => clearInterval(timer);
  }, [id, status, refetch]);

  // Once the result is known, refresh the fee balances shown below.
  useEffect(() => {
    if (status && status !== 'PENDING') dispatch(api.util.invalidateTags(['Fees']));
  }, [status, dispatch]);

  if (!id || !data) return null;
  const view = returnMessage(data.status);
  return (
    <div className={`banner banner-${view.tone}`} role="status">
      <span>{view.text}</span>
      <button className="btn-link" onClick={() => setParams({})}>Dismiss</button>
    </div>
  );
}

function FeesCard({ studentId }) {
  const { data: ledger, error } = useGetChildFeesQuery(studentId);
  const [receipt, setReceipt] = useState(null);
  const [off, setOff] = useState([]);                       // fees the parent un-ticked
  const [startPayment, { isLoading: starting, error: payError }] = useStartPaymentMutation();

  const payable = ledger ? payableCharges(ledger) : [];
  const chosen = payable.filter((c) => !off.includes(c.id)).map((c) => c.id);
  const chosenKey = chosen.join(',');
  // One key per selection: clicking Pay twice returns the same checkout instead of two payments.
  const idempotencyKey = useMemo(() => newKey(), [chosenKey]);

  if (error) return <ErrorNotice error={error} />;
  if (!ledger || ledger.charges.length === 0) return null;   // the school has not billed anything yet

  const toggle = (id) => setOff(off.includes(id) ? off.filter((x) => x !== id) : [...off, id]);
  const total = selectedTotal(payable, chosen);

  const pay = async () => {
    try {
      const result = await startPayment({ studentId, chargeIds: chosen, idempotencyKey }).unwrap();
      const url = safeRedirectUrl(result.checkoutUrl);
      if (url) window.location.assign(url);
    } catch { /* shown below */ }
  };

  return (
    <section className="card">
      <div className="page-header" style={{ marginBottom: 12 }}>
        <h2 style={{ margin: 0 }}>Fees</h2>
        <Chip tone={ledger.balance > 0 ? 'warn' : 'ok'}>
          {ledger.balance > 0 ? `${formatMoney(ledger.balance, ledger.currency)} to pay` : 'All paid'}
        </Chip>
      </div>
      <ChargesTable ledger={ledger} />

      {payable.length > 0 && (
        <div className="card card-accent" style={{ marginTop: 16, marginBottom: 0 }}>
          <h2>Pay online</h2>
          {payable.map((c) => (
            <label key={c.id} className="check">
              <input type="checkbox" checked={!off.includes(c.id)} onChange={() => toggle(c.id)} />
              <span>{c.name} <span className="muted">· {formatMoney(c.balance, ledger.currency)}</span></span>
            </label>
          ))}
          <button className="btn btn-primary" disabled={starting || chosen.length === 0} onClick={pay} style={{ marginTop: 8 }}>
            {starting ? 'Please wait…' : `Pay ${formatMoney(total, ledger.currency)} online`}
          </button>
          <ErrorNotice error={payError} />
        </div>
      )}

      <h2 style={{ marginTop: 20 }}>Payments</h2>
      <PaymentsList ledger={ledger} onReceipt={setReceipt} />
      <Receipt ledger={ledger} payment={receipt} onClose={() => setReceipt(null)} />
    </section>
  );
}

function ResultsCard({ studentId }) {
  const { data: exams = [] } = useListChildResultsQuery(studentId);
  const [openId, setOpenId] = useState(null);
  if (exams.length === 0) return null;   // nothing has been published yet

  return (
    <section className="card">
      <h2>Results</h2>
      <ul className="checklist">
        {exams.map((e) => (
          <li key={e.examId}>
            <span><strong>{e.name}</strong> <span className="muted">· {fmt(e.percent)}%</span></span>
            <span className="row-actions">
              {e.grade && <Chip tone="info">{e.grade}</Chip>}
              <Chip tone={RESULT[e.result].tone}>{RESULT[e.result].label}</Chip>
              <button className="btn btn-secondary" onClick={() => setOpenId(e.examId)}>Report card</button>
            </span>
          </li>
        ))}
      </ul>
      <ReportCardDialog studentId={studentId} examId={openId} onClose={() => setOpenId(null)} />
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
      <ResultsCard studentId={studentId} />
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
      <PaymentBanner></PaymentBanner>
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