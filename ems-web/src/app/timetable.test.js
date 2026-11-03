import { describe, expect, it } from 'vitest';
import {
  DAYS, hhmm, indexEntries, indexLessons, slotKey, subjectsFor, teachersFor, timeRange, todayDayNumber, validatePeriod,
} from './timetable';

describe('time and day helpers', () => {
  it('trims seconds from times', () => {
    expect(hhmm('08:00:00')).toBe('08:00');
    expect(hhmm(null)).toBe('');
    expect(timeRange({ startTime: '08:00:00', endTime: '08:40:00' })).toBe('08:00–08:40');
  });
  it('numbers Monday as 1 and Sunday as 7, like the server', () => {
    expect(todayDayNumber(new Date(2026, 9, 26))).toBe(1);   // Monday
    expect(todayDayNumber(new Date(2026, 9, 25))).toBe(7);   // Sunday
    expect(todayDayNumber(new Date(2026, 9, 31))).toBe(6);   // Saturday
  });
  it('shows Monday to Saturday', () => {
    expect(DAYS.map((d) => d.short)).toEqual(['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']);
  });
});

describe('indexing', () => {
  it('finds lessons by day and period', () => {
    const entries = indexEntries([{ day: 1, periodId: 'p1', subjectName: 'English' }]);
    expect(entries[slotKey(1, 'p1')].subjectName).toBe('English');
    expect(entries[slotKey(2, 'p1')]).toBeUndefined();
    expect(indexLessons(undefined)).toEqual({});
  });
});

describe('who can be scheduled', () => {
  const staff = [
    { id: 't1', name: 'Meena', active: true, assignments: [{ sectionId: 's1', subjectId: 'eng', subjectName: 'English' }, { sectionId: 's1', classTeacher: true }] },
    { id: 't2', name: 'Ravi', active: true, assignments: [{ sectionId: 's1', subjectId: 'mat', subjectName: 'Mathematics' }, { sectionId: 's2', subjectId: 'eng', subjectName: 'English' }] },
    { id: 't3', name: 'Left', active: false, assignments: [{ sectionId: 's1', subjectId: 'sci', subjectName: 'Science' }] },
  ];

  it('offers only subjects with an active assigned teacher in that section', () => {
    expect(subjectsFor(staff, 's1').map((s) => s.name)).toEqual(['English', 'Mathematics']);
    expect(subjectsFor(staff, 's2').map((s) => s.name)).toEqual(['English']);
    expect(subjectsFor(staff, 'nowhere')).toEqual([]);
  });
  it('offers only the teachers assigned to that subject in that section', () => {
    expect(teachersFor(staff, 's1', 'eng').map((t) => t.name)).toEqual(['Meena']);
    expect(teachersFor(staff, 's2', 'eng').map((t) => t.name)).toEqual(['Ravi']);
    expect(teachersFor(staff, 's1', 'sci')).toEqual([]);   // inactive
  });
});

describe('validatePeriod', () => {
  const ok = { number: '1', name: 'Period 1', startTime: '08:00', endTime: '08:40' };
  it('accepts a sensible period', () => expect(validatePeriod(ok)).toBeNull());
  it('rejects bad numbers, empty names, missing or backwards times', () => {
    expect(validatePeriod({ ...ok, number: '0' })).toMatch(/between 1 and 20/);
    expect(validatePeriod({ ...ok, number: '2.5' })).toMatch(/between 1 and 20/);
    expect(validatePeriod({ ...ok, name: ' ' })).toMatch(/name/);
    expect(validatePeriod({ ...ok, endTime: '' })).toMatch(/start and end/);
    expect(validatePeriod({ ...ok, endTime: '08:00' })).toMatch(/end after/);
  });
});