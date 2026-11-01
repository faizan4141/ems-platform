import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { formatMoney } from '../app/fees';
import { useGetStudentLedgerQuery, useVoidPaymentMutation } from '../services/feesApi';
import { ChargesTable, PaymentsList } from '../components/LedgerTables';
import PaymentForm from '../components/PaymentForm';
import Receipt from '../components/Receipt';
import { ErrorNotice, PageHeader } from '../components/ui';
import VoidDialog from '../components/VoidDialog';

export default function StudentFeesPage() {
  const { id } = useParams();
  const dispatch = useDispatch();
  const { data: ledger, isLoading, error } = useGetStudentLedgerQuery(id);
  const [voidPayment, { isLoading: voiding, error: voidError, reset: resetVoid }] = useVoidPaymentMutation();
  const [voidTarget, setVoidTarget] = useState(null);
  const [payingId, setPayingId] = useState(null);
  const [receipt, setReceipt] = useState(null);

  if (isLoading) return <div className="skeleton" style={{ width: '40%' }} />;
  if (error) return <><Link to="/fees" className="btn-link">← Fees</Link><ErrorNotice error={error} /></>;

  const paying = ledger.charges.find((c) => c.id === payingId);
  const money = (v) => formatMoney(v, ledger.currency);

  const confirmVoid = async (reason) => {
    try {
      await voidPayment({ paymentId: voidTarget.id, reason }).unwrap();
      dispatch(pushToast({ text: `Receipt ${voidTarget.receiptNo} voided` }));
      setVoidTarget(null);
    } catch { /* shown inside the dialog */ }
  };

  const closeVoid = () => {
    setVoidTarget(null);
    resetVoid();
  };

  return (
    <>
      <Link to="/fees" className="btn-link">← Fees</Link>
      <div style={{ height: 12 }} />
      <PageHeader title={ledger.studentName} subtitle={`Admission no. ${ledger.admissionNo}`}
        action={<Link className="btn btn-secondary" to={`/students/${ledger.studentId}`}>Student profile</Link>} />

      <div className="kpis">
        <div className="card"><div className="muted">Billed</div><div className="kpi-value">{money(ledger.billed)}</div></div>
        <div className="card"><div className="muted">Paid</div><div className="kpi-value">{money(ledger.paid)}</div></div>
        <div className="card"><div className="muted">Balance</div><div className="kpi-value">{money(ledger.balance)}</div></div>
      </div>

      <section className="card">
        <h2>Fees</h2>
        {ledger.charges.length === 0
          ? <p className="muted">No fees have been generated for this student yet.</p>
          : <ChargesTable ledger={ledger} renderAction={(c) => c.balance > 0 && (
              <button className="btn btn-primary" onClick={() => setPayingId(c.id)}>Record payment</button>
            )} />}
      </section>

      {paying && <PaymentForm key={paying.id} charge={paying} currency={ledger.currency}
        onDone={(result) => { setPayingId(null); if (result) setReceipt(result); }} />}

      <section className="card">
        <h2>Payments</h2>
        <PaymentsList ledger={ledger} onReceipt={setReceipt}
          renderAction={(p) => !p.voided && (
            <button className="btn btn-secondary" onClick={() => setVoidTarget(p)}>Void</button>
          )} />
      </section>

      <Receipt ledger={ledger} payment={receipt} onClose={() => setReceipt(null)} />
      <VoidDialog payment={voidTarget} busy={voiding} error={voidError} onConfirm={confirmVoid} onClose={closeVoid} />
    </>
  );
}