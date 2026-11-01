import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { formatMoney } from '../app/fees';
import { useCompleteSimulatedMutation, useGetAttemptQuery } from '../services/feesApi';
import { ErrorNotice } from '../components/ui';

export default function SimulatedCheckoutPage() {
  const { providerRef } = useParams();
  const [params] = useSearchParams();
  const attemptId = params.get('attempt');
  const navigate = useNavigate();
  const { data: attempt, error } = useGetAttemptQuery(attemptId, { skip: !attemptId });
  const [complete, { isLoading, error: completeError }] = useCompleteSimulatedMutation();

  const finish = async (outcome) => {
    try {
      await complete({ providerRef, outcome }).unwrap();
      navigate(`/parent?payment=${attemptId}`);
    } catch { /* shown below */ }
  };

  return (
    <section className="card checkout">
      <div className="sim-note"><strong>Test checkout.</strong> No real money moves. This page stands in for the payment provider.</div>
      <h1 style={{ fontSize: '1.3rem' }}>Complete your payment</h1>
      <ErrorNotice error={error || completeError} />
      {attempt && (
        <>
          <p className="kpi-value">{formatMoney(attempt.amount, attempt.currency)}</p>
          {attempt.status !== 'PENDING' ? (
            <p className="muted">This payment is already {attempt.status.toLowerCase()}.</p>
          ) : (
            <div className="dialog-actions" style={{ justifyContent: 'flex-start' }}>
              <button className="btn btn-primary" disabled={isLoading} onClick={() => finish('success')}>Pay now (simulate success)</button>
              <button className="btn btn-secondary" disabled={isLoading} onClick={() => finish('failure')}>Simulate a failure</button>
            </div>
          )}
        </>
      )}
      <p style={{ marginBottom: 0 }}><button className="btn-link" onClick={() => navigate('/parent')}>Cancel and go back</button></p>
    </section>
  );
}