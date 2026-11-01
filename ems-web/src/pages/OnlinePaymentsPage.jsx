import { Link } from 'react-router-dom';
import { ATTEMPT } from '../app/onlinePay';
import { formatMoney } from '../app/fees';
import { useListOnlinePaymentsQuery } from '../services/feesApi';
import { Chip, EmptyState, ErrorNotice, PageHeader, SkeletonRows } from '../components/ui';

export default function OnlinePaymentsPage() {
  const { data: rows, isLoading, error } = useListOnlinePaymentsQuery();
  const review = rows?.filter((r) => r.status === 'REVIEW').length ?? 0;

  return (
    <>
      <Link to="/fees" className="btn-link">← Fees</Link>
      <div style={{ height: 12 }} />
      <PageHeader title="Online payments" subtitle="Payments parents started from the portal." />
      {review > 0 && (
        <div className="banner banner-bad" role="alert">
          {review} payment{review === 1 ? ' needs' : 's need'} your attention. Money was received that could not be applied
          automatically. Check the note, then refund or correct it with your payment provider.
        </div>
      )}
      <section className="card">
        <ErrorNotice error={error} />
        {rows?.length === 0 && <EmptyState title="No online payments yet" text="They appear here when a parent starts to pay." />}
        {(isLoading || rows?.length > 0) && (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>When</th><th>Student</th><th>Amount</th><th>Status</th><th>Note</th></tr></thead>
              <tbody>
                {isLoading && <SkeletonRows cols={5} />}
                {rows?.map((r) => (
                  <tr key={r.id}>
                    <td className="muted">{new Date(r.createdAt).toLocaleString()}</td>
                    <td><Link to={`/fees/students/${r.studentId}`}><strong>{r.studentName}</strong></Link>
                      <div className="muted">{r.admissionNo}</div></td>
                    <td>{formatMoney(r.amount, r.currency)}</td>
                    <td><Chip tone={ATTEMPT[r.status].tone}>{ATTEMPT[r.status].label}</Chip></td>
                    <td className="muted" style={{ whiteSpace: 'normal' }}>{r.note ?? '—'}</td>
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