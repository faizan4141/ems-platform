import { useEffect, useRef, useState } from 'react';
import { ErrorNotice, Field } from './ui';

export default function VoidDialog({ payment, busy, error, onConfirm, onClose }) {
  const ref = useRef(null);
  const [reason, setReason] = useState('');

  useEffect(() => {
    const dialog = ref.current;
    if (payment && !dialog.open) dialog.showModal();
    if (!payment && dialog.open) dialog.close();
  }, [payment]);

  const close = () => {
    setReason('');
    onClose();
  };

  const submit = (e) => {
    e.preventDefault();
    if (reason.trim()) onConfirm(reason.trim());
  };

  return (
    <dialog ref={ref} className="dialog" onClose={close} aria-labelledby="void-title">
      {payment && (
        <form onSubmit={submit}>
          <h2 id="void-title">Void receipt {payment.receiptNo}?</h2>
          <p className="muted" style={{ marginTop: 0 }}>
            The receipt stays on record, marked VOID, and the fee balance reopens. The receipt number is never reused.
          </p>
          <Field label="Reason (required)">
            <input value={reason} onChange={(e) => setReason(e.target.value)} maxLength={200} required autoFocus
                   placeholder="For example: entered twice" />
          </Field>
          <ErrorNotice error={error} />
          <div className="dialog-actions">
            <button type="button" className="btn btn-secondary" onClick={close}>Keep receipt</button>
            <button className="btn btn-danger" disabled={busy || !reason.trim()}>
              {busy ? 'Voiding…' : 'Void receipt'}
            </button>
          </div>
        </form>
      )}
    </dialog>
  );
}