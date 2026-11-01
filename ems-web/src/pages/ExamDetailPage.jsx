import { useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { useAuth } from '../features/auth/useAuth';
import { RESULT, cellKey, cellText, fmt, parseMark, pendingChanges } from '../app/exams';
import { useListClassesQuery } from '../services/academicApi';
import {
  useGetAuditQuery, useGetExamQuery, useGetGridQuery, useGetResultsQuery, usePublishExamMutation,
  useReopenExamMutation, useSaveMarksMutation,
} from '../services/examsApi';
import ConfirmDialog from '../components/ConfirmDialog';
import { Chip, EmptyState, ErrorNotice, Field, PageHeader } from '../components/ui';

function MarksGrid({ grid, editable }) {
  const dispatch = useDispatch();
  const [edits, setEdits] = useState({});
  const [save, { isLoading, error }] = useSaveMarksMutation();
  const { entries, invalid } = useMemo(() => pendingChanges(grid, edits), [grid, edits]);
  const subjects = grid.exam.subjects;

  if (grid.rows.length === 0) {
    return <EmptyState title="No students" text="There are no active students in this class or section." />;
  }

  const onSave = async () => {
    try {
      const result = await save({ examId: grid.exam.id, entries }).unwrap();
      setEdits({});
      dispatch(pushToast({ text: `${result.changed} mark${result.changed === 1 ? '' : 's'} saved` }));
    } catch { /* shown below */ }
  };

  return (
    <>
      {editable && <p className="muted" style={{ marginTop: 0 }}>Type a mark, or <strong>AB</strong> if the student was absent. Clear a box to remove its mark.</p>}
      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr>
              <th>Student</th>
              {subjects.map((s) => <th key={s.examSubjectId}>{s.subjectName}<div className="muted">out of {fmt(s.maxMarks)}</div></th>)}
            </tr>
          </thead>
          <tbody>
            {grid.rows.map((row) => (
              <tr key={row.studentId}>
                <td><strong>{row.name}</strong><div className="muted">{row.admissionNo}</div></td>
                {subjects.map((s) => {
                  const key = cellKey(row.studentId, s.examSubjectId);
                  const text = key in edits ? edits[key] : cellText(row.cells[s.examSubjectId]);
                  return (
                    <td key={key}>
                      {editable ? (
                        <input className="marks-input" inputMode="decimal" value={text}
                               aria-label={`${row.name}, ${s.subjectName}`} aria-invalid={!parseMark(text, s.maxMarks).ok}
                               onChange={(e) => setEdits({ ...edits, [key]: e.target.value })} />
                      ) : (text || '—')}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {editable && (
        <div className="row-actions" style={{ marginTop: 12 }}>
          <button className="btn btn-primary" disabled={isLoading || entries.length === 0 || invalid.length > 0} onClick={onSave}>
            {isLoading ? 'Saving…' : `Save ${entries.length || ''} change${entries.length === 1 ? '' : 's'}`}
          </button>
          {invalid.length > 0 && <span className="muted">Fix the highlighted marks first.</span>}
        </div>
      )}
      <ErrorNotice error={error} />
    </>
  );
}

export default function ExamDetailPage() {
  const { id } = useParams();
  const dispatch = useDispatch();
  const isAdmin = useAuth().roles.includes('TENANT_ADMIN');
  const { data: exam, error } = useGetExamQuery(id);
  const { data: classes = [] } = useListClassesQuery();
  const [sectionId, setSectionId] = useState('');
  const { data: grid } = useGetGridQuery({ examId: id, sectionId }, { skip: !exam });
  const { data: results } = useGetResultsQuery({ examId: id, sectionId }, { skip: !exam });
  const { data: audit = [] } = useGetAuditQuery(id, { skip: !isAdmin });
  const [publish, publishState] = usePublishExamMutation();
  const [reopen, reopenState] = useReopenExamMutation();
  const [dialog, setDialog] = useState(null);   // 'publish' | 'reopen'

  if (error) return <><Link to="/exams" className="btn-link">← Exams</Link><ErrorNotice error={error} /></>;
  if (!exam) return <div className="skeleton" style={{ width: '40%' }} />;

  const sections = classes.find((c) => c.id === exam.classId)?.sections ?? [];
  const draft = exam.status === 'DRAFT';

  const doPublish = async () => {
    try {
      await publish(id).unwrap();
      dispatch(pushToast({ text: 'Results published. Parents can now see them.' }));
      setDialog(null);
    } catch { /* shown inside the dialog */ }
  };

  const doReopen = async (reason) => {
    try {
      await reopen({ examId: id, reason }).unwrap();
      dispatch(pushToast({ text: 'Exam reopened for editing' }));
      setDialog(null);
    } catch { /* shown inside the dialog */ }
  };

  return (
    <>
      <Link to="/exams" className="btn-link">← Exams</Link>
      <div style={{ height: 12 }} />
      <PageHeader title={exam.name} subtitle={exam.className}
        action={(
          <div className="row-actions">
            <Chip tone={draft ? 'warn' : 'ok'}>{draft ? 'Draft' : 'Published'}</Chip>
            {isAdmin && draft && <button className="btn btn-primary" onClick={() => setDialog('publish')}>Publish results</button>}
            {isAdmin && !draft && <button className="btn btn-secondary" onClick={() => setDialog('reopen')}>Reopen</button>}
          </div>
        )} />

      <section className="card">
        <div className="toolbar">
          <h2 style={{ margin: 0, flex: 1 }}>Marks</h2>
          {sections.length > 0 && (
            <Field label="Section">
              <select value={sectionId} onChange={(e) => setSectionId(e.target.value)}>
                <option value="">All sections</option>
                {sections.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </Field>
          )}
        </div>
        {!draft && <div className="banner banner-warn">Results are published and locked. An admin can reopen the exam to correct a mark.</div>}
        {grid ? <MarksGrid key={`${sectionId}|${exam.status}`} grid={grid} editable={draft} /> : <div className="skeleton" style={{ width: '60%' }} />}
      </section>

      <section className="card">
        <h2>Results so far</h2>
        {!results || results.rows.length === 0 ? <p className="muted">No students yet.</p> : (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Student</th><th>Total</th><th>%</th><th>Grade</th><th>Result</th><th></th></tr></thead>
              <tbody>
                {results.rows.map((r) => (
                  <tr key={r.studentId}>
                    <td><strong>{r.name}</strong><div className="muted">{r.admissionNo}</div></td>
                    <td>{r.complete ? `${fmt(r.totalMarks)} / ${fmt(r.totalMax)}` : '—'}</td>
                    <td>{fmt(r.percent)}</td>
                    <td>{r.grade ?? '—'}</td>
                    <td><Chip tone={RESULT[r.result].tone}>{RESULT[r.result].label}</Chip></td>
                    <td><Link className="btn btn-secondary" to={`/exams/${id}/report/${r.studentId}`}>Report card</Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {isAdmin && (
        <section className="card">
          <h2>Audit trail</h2>
          {audit.length === 0 ? <p className="muted">Nothing recorded yet.</p> : (
            <ul className="checklist">
              {audit.map((a, i) => (
                <li key={i}>
                  <span><strong>{a.action}</strong> <span className="muted">· {a.actor}</span><br /><span className="muted">{a.detail}</span></span>
                  <span className="muted">{new Date(a.at).toLocaleString()}</span>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <ConfirmDialog open={dialog === 'publish'} title="Publish these results?" confirmLabel="Publish"
        text="Marks will be locked and parents will be able to see the results and report cards."
        busy={publishState.isLoading} error={publishState.error} onConfirm={doPublish} onClose={() => { setDialog(null); publishState.reset(); }} />
      <ConfirmDialog open={dialog === 'reopen'} title="Reopen this exam?" confirmLabel="Reopen" needsReason
        text="Parents will stop seeing these results until you publish again. The reason is saved in the audit trail."
        busy={reopenState.isLoading} error={reopenState.error} onConfirm={doReopen} onClose={() => { setDialog(null); reopenState.reset(); }} />
    </>
  );
}