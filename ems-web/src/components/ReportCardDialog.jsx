import { useEffect, useRef } from 'react';
import { useGetChildReportCardQuery } from '../services/examsApi';
import ReportCardView from './ReportCardView';
import { ErrorNotice } from './ui';

export default function ReportCardDialog({ studentId, examId, onClose }) {
  const ref = useRef(null);
  const { data: card, error } = useGetChildReportCardQuery({ studentId, examId }, { skip: !examId });

  useEffect(() => {
    const dialog = ref.current;
    if (examId && !dialog.open) dialog.showModal();
    if (!examId && dialog.open) dialog.close();
  }, [examId]);

  return (
    <dialog ref={ref} className="dialog receipt" onClose={onClose} aria-labelledby="report-title">
      <span id="report-title" className="sr-only">Report card</span>
      <ErrorNotice error={error} />
      {card && <ReportCardView card={card} />}
      <div className="dialog-actions">
        <button className="btn btn-secondary" onClick={onClose}>Close</button>
        <button className="btn btn-primary" disabled={!card} onClick={() => window.print()}>Print</button>
      </div>
    </dialog>
  );
}