import { useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { METHODS, NEEDS_REFERENCE, formatMoney, newKey, validateAmount } from '../app/fees';
import { useRecordPaymentMutation } from '../services/feesApi';
import { ErrorNotice, Field } from './ui';

export default function PaymentForm({ charge, currency, onDone }) {
  const dispatch = useDispatch();
  const keyRef = useRef(newKey());
  const [form, setForm] = useState({ amount: String(charge.balance), method: 'CASH', reference: '', paidOn: '' });
  const [record, { isLoading, error }] = useRecordPaymentMutation();

  const problem = validateAmount(form.amount, charge.balance);
  const needsReference = NEEDS_REFERENCE.includes(form.method);
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    if (problem) return;
    try {
      const result = await record({
        chargeId: charge.id, amount: form.amount, method: form.method,
        reference: form.reference || null, paidOn: form.paidOn || null, idempotencyKey: keyRef.current,
      }).unwrap();
      keyRef.current = newKey();   // only after success; a failed attempt may be retried safely with the same key
      dispatch(pushToast({ text: `Payment recorded. Receipt ${result.receiptNo}` }));
      onDone(result);
    } catch { /* shown below */ }
  };

  return (
    <section className="card card-accent">
      <h2>Record a payment: {charge.name}</h2>
      <p className="muted" style={{ marginTop: 0 }}>Balance {formatMoney(charge.balance, currency)}</p>
      <form onSubmit={onSubmit} className="form-grid">
        <Field label="Amount"><input name="amount" value={form.amount} onChange={onChange} inputMode="decimal" required /></Field>
        <Field label="Method">
          <select name="method" value={form.method} onChange={onChange}>
            {METHODS.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
          </select>
        </Field>
        <Field label={needsReference ? 'Reference (required)' : 'Reference (optional)'}>
          <input name="reference" value={form.reference} onChange={onChange} maxLength={100} required={needsReference} />
        </Field>
        <Field label="Paid on (default today)"><input type="date" name="paidOn" value={form.paidOn} onChange={onChange} /></Field>
        <button className="btn btn-primary" disabled={isLoading || !!problem}>{isLoading ? 'Saving…' : 'Record payment'}</button>
        <button type="button" className="btn btn-secondary" onClick={() => onDone(null)}>Cancel</button>
      </form>
      {form.amount && problem && <div className="notice-error">{problem}</div>}
      <ErrorNotice error={error} />
    </section>
  );
}