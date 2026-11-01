export const RESULT = {
  PASS: { label: 'Pass', tone: 'ok' },
  FAIL: { label: 'Fail', tone: 'bad' },
  INCOMPLETE: { label: 'Incomplete', tone: 'warn' },
};

export const cellKey = (studentId, examSubjectId) => `${studentId}|${examSubjectId}`;

// What is shown in a grid cell: the mark, AB for absent, or empty.
export function cellText(cell) {
  if (!cell) return '';
  if (cell.absent) return 'AB';
  return cell.marks == null ? '' : String(cell.marks);
}

// '' clears an entry, A / AB / absent marks the student absent, anything else must be a valid mark.
export function parseMark(text, max) {
  const t = String(text ?? '').trim();
  if (t === '') return { ok: true, marks: null, absent: false };
  if (/^(a|ab|absent)$/i.test(t)) return { ok: true, marks: null, absent: true };
  if (!/^\d+(\.\d{1,2})?$/.test(t)) return { ok: false, error: 'Enter a number (up to 2 decimals) or AB' };
  if (Number(t) > Number(max)) return { ok: false, error: `At most ${max}` };
  return { ok: true, marks: t, absent: false };
}

// '85' and '85.00' are the same mark, so a cell is only "changed" when its meaning changes.
export function normalize(text) {
  const t = String(text ?? '').trim();
  if (t === '') return '';
  if (/^(a|ab|absent)$/i.test(t)) return 'AB';
  const n = Number(t);
  return Number.isNaN(n) ? t : String(n);
}

export function pendingChanges(grid, edits) {
  const maxBy = Object.fromEntries(grid.exam.subjects.map((s) => [s.examSubjectId, s.maxMarks]));
  const entries = [];
  const invalid = [];
  for (const row of grid.rows) {
    for (const subjectId of Object.keys(maxBy)) {
      const key = cellKey(row.studentId, subjectId);
      if (!(key in edits)) continue;
      if (normalize(edits[key]) === normalize(cellText(row.cells[subjectId]))) continue;
      const parsed = parseMark(edits[key], maxBy[subjectId]);
      if (!parsed.ok) {
        invalid.push(key);
      } else {
        entries.push({ studentId: row.studentId, examSubjectId: subjectId, marks: parsed.marks, absent: parsed.absent });
      }
    }
  }
  return { entries, invalid };
}

export const fmt = (value) => (value == null ? '—' : Number(value).toLocaleString(undefined, { maximumFractionDigits: 2 }));

export function validateScale(rows) {
  if (rows.length === 0) return 'Add at least one grade';
  const mins = rows.map((r) => Number(r.min));
  if (rows.some((r) => r.min === '' || Number.isNaN(Number(r.min)) || Number(r.min) < 0 || Number(r.min) > 100)) {
    return 'Each grade needs a starting percentage between 0 and 100';
  }
  if (rows.some((r) => !r.grade.trim())) return 'Each grade needs a name';
  if (new Set(mins).size !== mins.length) return 'Two grades start at the same percentage';
  if (!mins.includes(0)) return 'The lowest grade must start at 0%';
  return null;
}