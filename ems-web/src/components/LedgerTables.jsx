import { FEE_STATUS, formatMoney, methodLabel } from '../app/fees';
import { shortDate } from '../app/parent';
import { Chip } from './ui';

export function ChargesTable({ ledger, renderAction }) {
  const money = (value) => formatMoney(value, ledger.currency);
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr><th>Fee</th><th>Due</th><th>Amount</th><th>Paid</th><th>Balance</th><th>Status</th>{renderAction && <th></th>}</tr>
        </thead>
        <tbody>
          {ledger.charges.map((c) => (
            <tr key={c.id}>
              <td><strong>{c.name}</strong></td>
              <td className="muted">{shortDate(c.dueDate)}</td>
              <td>{money(c.amount)}</td>
              <td>{money(c.paid)}</td>
              <td>{money(c.balance)}</td>
              <td><Chip tone={FEE_STATUS[c.status].tone}>{FEE_STATUS[c.status].label}</Chip></td>
              {renderAction && <td>{renderAction(c)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function PaymentsList({ ledger, onReceipt, renderAction }) {
  if (ledger.payments.length === 0) return <p className="muted">No payments yet.</p>;
  return (
    <ul className="checklist">
      {ledger.payments.map((p) => (
        <li key={p.id}>
          <span>
            <strong>{formatMoney(p.amount, ledger.currency)}</strong>{' '}
            <span className="muted">· {p.chargeName} · {methodLabel(p.method)} · {shortDate(p.paidOn)}</span><br />
            <span className="muted">{p.receiptNo}</span> {p.voided && <Chip tone="bad">Voided</Chip>}
          </span>
          <span style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-secondary" onClick={() => onReceipt(p)}>Receipt</button>
            {renderAction?.(p)}
          </span>
        </li>
      ))}
    </ul>
  );
}