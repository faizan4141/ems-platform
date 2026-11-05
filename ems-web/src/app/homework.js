import { shortDate } from './parent';

export const KIND = {
  HOMEWORK: { label: 'Homework', tone: 'info' },
  NOTE: { label: 'Class note', tone: 'neutral' },
};

export const MAX_LINKS = 5;

// Only ever show web links as clickable: never javascript: or anything else.
export function safeLink(url) {
  try {
    const parsed = new URL(String(url ?? '').trim());
    const web = ['http:', 'https:'].includes(parsed.protocol);
    return web && !parsed.username && !parsed.password ? parsed.toString() : null;
  } catch {
    return null;
  }
}

export function linkLabel(url) {
  try {
    return new URL(url).hostname.replace(/^www\./, '');
  } catch {
    return url;
  }
}

export const linksFromText = (text = '') => text.split('\n').map((l) => l.trim()).filter(Boolean);
export const linksToText = (links = []) => links.join('\n');

export function isoDate(date = new Date()) {
  const p = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}`;
}

const toDays = (iso) => {
  const [y, m, d] = iso.split('-').map(Number);
  return Math.floor(Date.UTC(y, m - 1, d) / 86400000);
};

export const daysUntil = (due, today) => toDays(due) - toDays(today);

// today comes from the server (the school's date), so the label never depends on the phone's time zone.
export function dueStatus(due, today) {
  const n = daysUntil(due, today);
  if (n < 0) return { key: 'overdue', label: 'Overdue', tone: 'bad' };
  if (n === 0) return { key: 'today', label: 'Due today', tone: 'warn' };
  if (n === 1) return { key: 'soon', label: 'Due tomorrow', tone: 'warn' };
  if (n <= 3) return { key: 'soon', label: `Due ${shortDate(due)}`, tone: 'info' };
  return { key: 'later', label: `Due ${shortDate(due)}`, tone: 'neutral' };
}

// Homework still to do comes first, soonest due first. Notes and finished-or-overdue items follow, newest first.
export function orderForParents(items, today) {
  const todo = (i) => i.kind === 'HOMEWORK' && i.dueDate >= today;
  const upcoming = items.filter(todo).sort((a, b) => a.dueDate.localeCompare(b.dueDate));
  const rest = items.filter((i) => !todo(i)).sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  return [...upcoming, ...rest];
}

// Admins choose from every subject. A class teacher may post any subject for their section, a subject teacher only theirs.
export function postableSubjects({ isAdmin, allSubjects = [], teaching, sectionId }) {
  if (isAdmin) return allSubjects;
  const section = teaching?.sections?.find((s) => s.sectionId === sectionId);
  if (!section) return [];
  return section.classTeacher ? allSubjects : section.subjects;
}

export function validateHomework({ kind, title, description, dueDate, linksText }) {
  if (!title.trim()) return 'Give it a title';
  if (!description.trim()) return 'Write the instructions or notes';
  if (kind === 'HOMEWORK' && !dueDate) return 'Choose a due date';
  const links = linksFromText(linksText);
  if (links.length > MAX_LINKS) return `Add at most ${MAX_LINKS} links`;
  const bad = links.find((l) => !safeLink(l));
  if (bad) return `“${bad}” is not a valid web link. It must start with http:// or https://`;
  return null;
}

// The section the form really uses: the chosen one if it's still on offer, otherwise the first one.
export const effectiveSection = (chosen, options) =>
  (options.some((o) => o.id === chosen) ? chosen : options[0]?.id ?? '');

// The subject the form really uses: the chosen one only if it's allowed for this section.
export const effectiveSubject = (chosen, allowed) => (allowed.some((s) => s.id === chosen) ? chosen : '');