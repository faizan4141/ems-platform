import { Link, useParams } from 'react-router-dom';
import { useGetReportCardQuery } from '../services/examsApi';
import ReportCardView from '../components/ReportCardView';
import { ErrorNotice } from '../components/ui';

export default function ReportCardPage() {
  const { examId, studentId } = useParams();
  const { data: card, error } = useGetReportCardQuery({ examId, studentId });

  return (
    <>
      <Link to={`/exams/${examId}`} className="btn-link">← Back to the exam</Link>
      <div style={{ height: 12 }} />
      <ErrorNotice error={error} />
      {card && (
        <section className="card receipt">
          <ReportCardView card={card} />
          <div className="dialog-actions"><button className="btn btn-primary" onClick={() => window.print()}>Print</button></div>
        </section>
      )}
    </>
  );
}