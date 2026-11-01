import { describe, expect, it } from 'vitest';
import { RESULT, cellText, fmt, normalize, parseMark, pendingChanges, validateScale } from './exams';

describe('parseMark', () => {
  it('accepts numbers up to the maximum, AB for absent, and empty to clear', () => {
    expect(parseMark('85.5', '100')).toEqual({ ok: true, marks: '85.5', absent: false });
    expect(parseMark('100', '100').ok).toBe(true);
    expect(parseMark('ab', '100')).toEqual({ ok: true, marks: null, absent: true });
    expect(parseMark('A', '100').absent).toBe(true);
    expect(parseMark('', '100')).toEqual({ ok: true, marks: null, absent: false });
  });
  it('rejects negatives, text, too many decimals and marks above the maximum', () => {
    expect(parseMark('-1', '100').ok).toBe(false);
    expect(parseMark('eighty', '100').ok).toBe(false);
    expect(parseMark('85.555', '100').ok).toBe(false);
    expect(parseMark('100.5', '100')).toMatchObject({ ok: false, error: 'At most 100' });
  });
});

describe('cells and changes', () => {
  const grid = {
    exam: { subjects: [{ examSubjectId: 's1', maxMarks: 50 }, { examSubjectId: 's2', maxMarks: 100 }] },
    rows: [{ studentId: 'a', cells: { s1: { marks: 40, absent: false }, s2: { marks: null, absent: true } } },
           { studentId: 'b', cells: {} }],
  };

  it('shows marks, AB and empty cells', () => {
    expect(cellText(grid.rows[0].cells.s1)).toBe('40');
    expect(cellText(grid.rows[0].cells.s2)).toBe('AB');
    expect(cellText(undefined)).toBe('');
  });
  it('treats 40 and 40.00 as the same mark', () => {
    expect(normalize('40.00')).toBe('40');
    expect(normalize('ab')).toBe('AB');
  });
  it('lists only the cells whose meaning really changed', () => {
    const edits = { 'a|s1': '40.0', 'a|s2': '55', 'b|s1': '45', 'b|s2': '' };
    const { entries, invalid } = pendingChanges(grid, edits);
    expect(invalid).toEqual([]);
    expect(entries).toEqual([
      { studentId: 'a', examSubjectId: 's2', marks: '55', absent: false },
      { studentId: 'b', examSubjectId: 's1', marks: '45', absent: false },
    ]);
  });
  it('reports invalid cells instead of sending them', () => {
    const { entries, invalid } = pendingChanges(grid, { 'b|s1': '99' });   // out of 50
    expect(entries).toEqual([]);
    expect(invalid).toEqual(['b|s1']);
  });
});

describe('validateScale', () => {
  const ok = [{ min: '0', grade: 'F' }, { min: '50', grade: 'P' }];
  it('accepts a sensible scale', () => expect(validateScale(ok)).toBeNull());
  it('needs a 0% band, unique starts and names for every grade', () => {
    expect(validateScale([{ min: '50', grade: 'P' }])).toMatch(/0%/);
    expect(validateScale([{ min: '0', grade: 'F' }, { min: '0', grade: 'E' }])).toMatch(/same percentage/);
    expect(validateScale([{ min: '0', grade: ' ' }])).toMatch(/name/);
    expect(validateScale([{ min: '120', grade: 'A' }])).toMatch(/between 0 and 100/);
    expect(validateScale([])).toMatch(/at least one/);
  });
});

describe('display helpers', () => {
  it('formats numbers and has a label for every result', () => {
    expect(fmt(null)).toBe('—');
    expect(fmt('66.670')).toMatch(/66\.67/);
    expect(Object.keys(RESULT).sort()).toEqual(['FAIL', 'INCOMPLETE', 'PASS']);
  });
});