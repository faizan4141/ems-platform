import { RESULT, fmt } from '../app/exams';
import { Chip } from './ui';

export default function ReportCardView({ card }) {
  const result = RESULT[card.result];
  return (
    <div className="report">
      <h2 style={{ marginBottom: 2 }}>{card.schoolName}</h2>
      <p className="muted" style={{ marginTop: 0 }}>
        Report card · {card.examName} {card.status === 'DRAFT' && <Chip tone="warn">Draft, not published</Chip>}
      </p>
      <dl className="detail-grid">
        <div><dt>Student</dt><dd>{card.studentName}</dd></div>
        <div><dt>Admission no.</dt><dd>{card.admissionNo}</dd></div>
        <div><dt>Class</dt><dd>{card.sectionName ? `${card.className} · ${card.sectionName}` : card.className}</dd></div>
      </dl>

      <div className="table-wrap" style={{ marginTop: 16 }}>
        <table className="table">
          <thead><tr><th>Subject</th><th>Max</th><th>Pass</th><th>Marks</th><th>%</th><th>Grade</th><th>Result</th></tr></thead>
          <tbody>
            {card.lines.map((l) => (
              <tr key={l.subject}>
                <td><strong>{l.subject}</strong></td>
                <td>{fmt(l.maxMarks)}</td>
                <td className="muted">{fmt(l.passMarks)}</td>
                <td>{l.absent ? 'AB' : fmt(l.marks)}</td>
                <td>{fmt(l.percent)}</td>
                <td>{l.grade ?? '—'}</td>
                <td>{l.passed == null ? '—' : <Chip tone={l.passed ? 'ok' : 'bad'}>{l.passed ? 'Pass' : 'Fail'}</Chip>}</td>
              </tr>
            ))}
            <tr>
              <td><strong>Total</strong></td>
              <td><strong>{fmt(card.totalMax)}</strong></td>
              <td />
              <td><strong>{fmt(card.totalMarks)}</strong></td>
              <td><strong>{fmt(card.percent)}</strong></td>
              <td><strong>{card.grade ?? '—'}</strong></td>
              <td><Chip tone={result.tone}>{result.label}</Chip></td>
            </tr>
          </tbody>
        </table>
      </div>
      {!card.complete && <p className="muted">Some marks have not been entered yet, so the overall result is not available.</p>}
    </div>
  );
}