export const DAYS = [
  { n: 1, short: 'Mon', label: 'Monday' }, { n: 2, short: 'Tue', label: 'Tuesday' },
  { n: 3, short: 'Wed', label: 'Wednesday' }, { n: 4, short: 'Thu', label: 'Thursday' },
  { n: 5, short: 'Fri', label: 'Friday' }, { n: 6, short: 'Sat', label: 'Saturday' },
];

// The server sends "08:00:00"; people read "08:00".
export const hhmm = (time) => (time ? String(time).slice(0, 5) : '');

export const timeRange = (p) => `${hhmm(p.startTime)}–${hhmm(p.endTime)}`;

export const slotKey = (day, periodId) => `${day}|${periodId}`;

export const indexEntries = (entries = []) => Object.fromEntries(entries.map((e) => [slotKey(e.day, e.periodId), e]));

export const indexLessons = (lessons = []) => Object.fromEntries(lessons.map((l) => [slotKey(l.day, l.periodId), l]));

// Subjects that have at least one active teacher assigned in this section: the only ones that can be scheduled.
export function subjectsFor(staff = [], sectionId) {
  const found = new Map();
  for (const member of staff) {
    if (!member.active) continue;
    for (const a of member.assignments) {
      if (a.sectionId === sectionId && a.subjectId) found.set(a.subjectId, a.subjectName);
    }
  }
  return [...found].map(([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name));
}

export const teachersFor = (staff = [], sectionId, subjectId) =>
  staff.filter((m) => m.active && m.assignments.some((a) => a.sectionId === sectionId && a.subjectId === subjectId));

// JavaScript counts Sunday as 0; the server counts Monday as 1 and Sunday as 7.
export function todayDayNumber(date = new Date()) {
  const d = date.getDay();
  return d === 0 ? 7 : d;
}

export function validatePeriod({ number, name, startTime, endTime }) {
  const n = Number(number);
  if (!Number.isInteger(n) || n < 1 || n > 20) return 'The period number must be between 1 and 20';
  if (!name.trim()) return 'Give the period a name';
  if (!startTime || !endTime) return 'Set a start and end time';
  if (endTime <= startTime) return 'A period must end after it starts';
  return null;
}