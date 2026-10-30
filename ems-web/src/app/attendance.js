export const STATUS_ORDER = ['PRESENT', 'ABSENT', 'LATE'];
export const STATUS_LABEL = { PRESENT: 'Present', ABSENT: 'Absent', LATE: 'Late' };
export const STATUS_TONE = { PRESENT: 'ok', ABSENT: 'bad', LATE: 'warn' };

export const nextStatus = (status) => STATUS_ORDER[(STATUS_ORDER.indexOf(status) + 1) % STATUS_ORDER.length];

export const initialStatuses = (students) => Object.fromEntries(students.map((s) => [s.studentId, s.status]));

export function countStatuses(statuses) {
  const counts = { PRESENT: 0, ABSENT: 0, LATE: 0 };
  Object.values(statuses).forEach((s) => { counts[s] += 1; });
  return counts;
}

// The server treats anyone not listed as present, so only exceptions are sent.
export const buildMarks = (statuses) =>
  Object.entries(statuses).filter(([, status]) => status !== 'PRESENT').map(([studentId, status]) => ({ studentId, status }));

export const isDirty = (initial, current) => Object.keys(initial).some((id) => initial[id] !== current[id]);

export const notifiedText = (n) => (n > 0 ? `Attendance saved. ${n} guardian${n === 1 ? '' : 's'} notified.` : 'Attendance saved.');