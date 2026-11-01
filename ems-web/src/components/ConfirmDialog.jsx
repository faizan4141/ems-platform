import { useEffect, useRef, useState } from 'react';
import { ErrorNotice, Field } from './ui';

export default function ConfirmDialog({ open, title, text, confirmLabel = 'Confirm', needsReason = false, busy, error, onConfirm, onClose }) {
  const ref = useRef(null);
  const [reason, setReason] = useState('');

  useEffect(() => {
    const dialog = ref.current;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  const close = () => {
    setReason('');
    onClose();
  };

  const submit = (e) => {
    e.preventDefault();
    onConfirm(reason.trim());
  };

  return (
    <dialog ref={ref} className="dialog" onClose={close} aria-labelledby="confirm-title">
      {open && (
        <form onSubmit={submit}>
          <h2 id="confirm-title">{title}</h2>
          <p className="muted" style={{ marginTop: 0 }}>{text}</p>
          {needsReason && (
            <Field label="Reason (required)">
              <input value={reason} onChange={(e) => setReason(e.target.value)} maxLength={200} required autoFocus />
            </Field>
          )}
          <ErrorNotice error={error} />
          <div className="dialog-actions">
            <button type="button" className="btn btn-secondary" onClick={close}>Cancel</button>
            <button className="btn btn-primary" disabled={busy || (needsReason && !reason.trim())}>
              {busy ? 'Please wait…' : confirmLabel}
            </button>
          </div>
        </form>
      )}
    </dialog>
  );
}