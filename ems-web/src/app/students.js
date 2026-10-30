export const CSV_COLUMNS = ['admission_no', 'first_name', 'last_name', 'date_of_birth', 'gender', 'class',
  'section', 'guardian_name', 'guardian_relationship', 'guardian_phone', 'guardian_email'];

export function csvTemplate() {
  const example = 'A-1001,Aarav,Sharma,2014-05-10,M,Class 8,A,Raj Sharma,FATHER,9876543210,raj@example.com';
  return `${CSV_COLUMNS.join(',')}\n${example}\n`;
}

export const fullName = (s) => `${s.firstName} ${s.lastName}`;

export const placementLabel = (s) => (s.className && s.sectionName ? `${s.className} · ${s.sectionName}` : 'Not assigned');

export function downloadTextFile(filename, text) {
  const url = URL.createObjectURL(new Blob([text], { type: 'text/csv;charset=utf-8' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

export function splitName(full = '') {
  const parts = full.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return { first: '', last: '' };
  if (parts.length === 1) return { first: parts[0], last: parts[0] };
  return { first: parts.slice(0, -1).join(' '), last: parts[parts.length - 1] };
}