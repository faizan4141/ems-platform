import { useEffect, useRef, useState } from 'react';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { csvTemplate, downloadTextFile } from '../app/students';
import { useImportStudentsMutation } from '../services/studentsApi';
import { ErrorNotice } from './ui';

export default function ImportDialog({ open, onClose }) {
  const dispatch = useDispatch();
  const dialogRef = useRef(null);
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [importStudents, { isLoading, error, reset }] = useImportStudentsMutation();

  useEffect(() => {
    const dialog = dialogRef.current;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  const close = () => {
    setFile(null);
    setResult(null);
    reset();
    onClose();
  };

  const onFile = (e) => {
    setFile(e.target.files[0] ?? null);
    setResult(null);
    reset();
  };

  const run = async (dryRun) => {
    try {
      const res = await importStudents({ file, dryRun }).unwrap();
      if (dryRun) {
        setResult(res);
      } else {
        dispatch(pushToast({ text: `${res.imported} students imported` }));
        close();
      }
    } catch { /* shown below */ }
  };

  const problems = result ? result.totalRows - result.validRows : 0;

  return (
    <dialog ref={dialogRef} className="dialog" onClose={close} aria-labelledby="import-title">
      <h2 id="import-title">Import students from CSV</h2>
      <p className="muted" style={{ marginTop: 0 }}>
        One row per student, with one guardian per row. Dates can be YYYY-MM-DD or DD/MM/YYYY.
        Class and section must already exist in the current academic year.{' '}
        <button type="button" className="btn-link" onClick={() => downloadTextFile('students-template.csv', csvTemplate())}>
          Download a template
        </button>
      </p>

      <input type="file" accept=".csv,text/csv" onChange={onFile} aria-label="CSV file" />
      <ErrorNotice error={error} />

      {result && (
        <div style={{ marginTop: 16 }}>
          <p>
            <strong>{result.totalRows}</strong> rows checked: <strong>{result.validRows}</strong> ready to import
            {problems > 0 && <>, <strong>{problems}</strong> need fixing</>}.
          </p>
          {result.errors.length > 0 && (
            <div className="table-wrap" style={{ maxHeight: 240, overflowY: 'auto' }}>
              <table className="table">
                <thead><tr><th>Row</th><th>Problem</th></tr></thead>
                <tbody>
                  {result.errors.map((err, i) => (
                    <tr key={i}><td>{err.row}</td><td style={{ whiteSpace: 'normal' }}>{err.message}</td></tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {problems > 0 && result.validRows > 0 && (
            <p className="muted">Rows with problems are skipped. Fix them in the file and import it again later.</p>
          )}
        </div>
      )}

      <div className="dialog-actions">
        <button type="button" className="btn btn-secondary" onClick={close}>Cancel</button>
        <button type="button" className="btn btn-secondary" disabled={!file || isLoading} onClick={() => run(true)}>
          {isLoading && !result ? 'Checking…' : 'Check file'}
        </button>
        <button type="button" className="btn btn-primary" disabled={!result || result.validRows === 0 || isLoading}
                onClick={() => run(false)}>
          {result ? `Import ${result.validRows} students` : 'Import'}
        </button>
      </div>
    </dialog>
  );
}