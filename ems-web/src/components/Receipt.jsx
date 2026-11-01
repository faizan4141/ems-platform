import { useEffect, useRef } from 'react';
import { formatMoney, methodLabel } from '../app/fees';
import { shortDate } from '../app/parent';
import { Chip } from './ui';

export default function Receipt({ ledger, payment, onClose }) {
  const ref = useRef(null);

  useEffect(() => {
    const dialog = ref.current;
    if (payment && !dialog.open) dialog.showModal();
    if (!payment && dialog.open) dialog.close();
  }, [payment]);

  return (
    <dialog ref={ref} className="dialog receipt" onClose={onClose} aria-labelledby="receipt-title">
      {payment && (
        <>
          <h2 id="receipt-title" style={{ marginBottom: 4 }}>{ledger.schoolName}</h2>
          <p className="muted" style={{ marginTop: 0 }}>
            Fee receipt {payment.voided && <Chip tone="bad">VOID</Chip>}
          </p>
          <dl className="detail-grid">
            <div><dt>Receipt no.</dt><dd>{payment.receiptNo}</dd></div>
            <div><dt>Date</dt><dd>{shortDate(payment.paidOn)}</dd></div>
            <div><dt>Student</dt><dd>{ledger.studentName} ({ledger.admissionNo})</dd></div>
            <div><dt>Class</dt><dd>{ledger.className && ledger.sectionName ? `${ledger.className} · ${ledger.sectionName}` : '—'}</dd></div>
            <div><dt>Fee</dt><dd>{payment.chargeName}</dd></div>
            <div><dt>Amount</dt><dd>{formatMoney(payment.amount, ledger.currency)}</dd></div>
            <div><dt>Method</dt><dd>{methodLabel(payment.method)}</dd></div>
            <div><dt>Reference</dt><dd>{payment.reference ?? '—'}</dd></div>
          </dl>
          {payment.voided && <p className="muted">Voided: {payment.voidReason}</p>}
        </>
      )}
      <div className="dialog-actions">
        <button className="btn btn-secondary" onClick={onClose}>Close</button>
        <button className="btn btn-primary" onClick={() => window.print()}>Print</button>
      </div>
    </dialog>
  );
}