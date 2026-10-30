import { describe, expect, it } from 'vitest';
import { buildMarks, countStatuses, initialStatuses, isDirty, nextStatus, notifiedText } from './attendance';

describe('attendance helpers', () => {
  it('cycles present, absent, late and back', () => {
    expect(nextStatus('PRESENT')).toBe('ABSENT');
    expect(nextStatus('ABSENT')).toBe('LATE');
    expect(nextStatus('LATE')).toBe('PRESENT');
  });

  it('counts statuses', () => {
    expect(countStatuses({ a: 'PRESENT', b: 'ABSENT', c: 'ABSENT', d: 'LATE' })).toEqual({ PRESENT: 1, ABSENT: 2, LATE: 1 });
  });

  it('sends only the exceptions', () => {
    expect(buildMarks({ a: 'PRESENT', b: 'ABSENT', c: 'LATE' })).toEqual([
      { studentId: 'b', status: 'ABSENT' }, { studentId: 'c', status: 'LATE' },
    ]);
  });

  it('detects unsaved changes against the server state', () => {
    const initial = initialStatuses([{ studentId: 'a', status: 'PRESENT' }, { studentId: 'b', status: 'ABSENT' }]);
    expect(isDirty(initial, { a: 'PRESENT', b: 'ABSENT' })).toBe(false);
    expect(isDirty(initial, { a: 'LATE', b: 'ABSENT' })).toBe(true);
  });

  it('words the confirmation', () => {
    expect(notifiedText(0)).toBe('Attendance saved.');
    expect(notifiedText(1)).toBe('Attendance saved. 1 guardian notified.');
    expect(notifiedText(3)).toBe('Attendance saved. 3 guardians notified.');
  });
});