import { describe, expect, it } from 'vitest';
import {
  daysUntil, dueStatus, isoDate, linksFromText, linksToText, orderForParents, postableSubjects, safeLink, validateHomework, effectiveSection, effectiveSubject,
} from './homework';

describe('safeLink', () => {
  it('allows ordinary web links only', () => {
    expect(safeLink('https://example.com/a?b=1')).toBe('https://example.com/a?b=1');
    expect(safeLink(' http://school.example ')).toBe('http://school.example/');
    for (const bad of ['javascript:alert(1)', 'data:text/html,hi', 'ftp://example.com', 'example.com', '', null, 'https://user:pw@example.com/']) {
      expect(safeLink(bad)).toBeNull();
    }
  });
});

describe('links as text', () => {
  it('turns lines into links and back, ignoring blanks', () => {
    expect(linksFromText(' https://a.com \n\n https://b.com ')).toEqual(['https://a.com', 'https://b.com']);
    expect(linksToText(['https://a.com', 'https://b.com'])).toBe('https://a.com\nhttps://b.com');
    expect(linksFromText()).toEqual([]);
  });
});

describe('due dates', () => {
  it('counts whole days between calendar dates', () => {
    expect(daysUntil('2026-11-02', '2026-10-30')).toBe(3);
    expect(daysUntil('2026-10-30', '2026-10-30')).toBe(0);
    expect(daysUntil('2026-10-29', '2026-10-30')).toBe(-1);
    expect(daysUntil('2027-01-01', '2026-12-31')).toBe(1);
  });
  it('words the status for parents', () => {
    expect(dueStatus('2026-10-29', '2026-10-30')).toMatchObject({ label: 'Overdue', tone: 'bad' });
    expect(dueStatus('2026-10-30', '2026-10-30')).toMatchObject({ label: 'Due today', tone: 'warn' });
    expect(dueStatus('2026-10-31', '2026-10-30')).toMatchObject({ label: 'Due tomorrow', tone: 'warn' });
    expect(dueStatus('2026-11-02', '2026-10-30').tone).toBe('info');
    expect(dueStatus('2026-11-20', '2026-10-30').tone).toBe('neutral');
  });
  it('formats the local date for the date picker', () => {
    expect(isoDate(new Date(2026, 9, 5))).toBe('2026-10-05');
  });
});

describe('orderForParents', () => {
  const items = [
    { id: 'old', kind: 'HOMEWORK', dueDate: '2026-10-20', createdAt: '2026-10-10T08:00:00Z' },
    { id: 'later', kind: 'HOMEWORK', dueDate: '2026-11-05', createdAt: '2026-10-28T08:00:00Z' },
    { id: 'note', kind: 'NOTE', dueDate: null, createdAt: '2026-10-29T08:00:00Z' },
    { id: 'soon', kind: 'HOMEWORK', dueDate: '2026-10-31', createdAt: '2026-10-27T08:00:00Z' },
    { id: 'today', kind: 'HOMEWORK', dueDate: '2026-10-30', createdAt: '2026-10-26T08:00:00Z' },
  ];
  it('puts homework still to do first, soonest due first, then the rest newest first', () => {
    expect(orderForParents(items, '2026-10-30').map((i) => i.id)).toEqual(['today', 'soon', 'later', 'note', 'old']);
  });
  it('does not change the original list', () => {
    const copy = [...items];
    orderForParents(items, '2026-10-30');
    expect(items).toEqual(copy);
  });
});

describe('postableSubjects', () => {
  const allSubjects = [{ id: 'eng', name: 'English' }, { id: 'mat', name: 'Mathematics' }];
  const teaching = { sections: [
    { sectionId: 's1', classTeacher: true, subjects: [] },
    { sectionId: 's2', classTeacher: false, subjects: [{ id: 'eng', name: 'English' }] },
  ] };
  it('lets admins pick any subject', () => {
    expect(postableSubjects({ isAdmin: true, allSubjects, sectionId: 's9' })).toEqual(allSubjects);
  });
  it('lets a class teacher pick any subject for their own section', () => {
    expect(postableSubjects({ isAdmin: false, allSubjects, teaching, sectionId: 's1' })).toEqual(allSubjects);
  });
  it('limits a subject teacher to their subjects, and gives nothing for other sections', () => {
    expect(postableSubjects({ isAdmin: false, allSubjects, teaching, sectionId: 's2' })).toEqual([{ id: 'eng', name: 'English' }]);
    expect(postableSubjects({ isAdmin: false, allSubjects, teaching, sectionId: 'other' })).toEqual([]);
    expect(postableSubjects({ isAdmin: false, allSubjects, sectionId: 's1' })).toEqual([]);
  });
});

describe('validateHomework', () => {
  const ok = { kind: 'HOMEWORK', title: 'Chapter 3', description: 'Read it', dueDate: '2026-11-02', linksText: '' };
  it('accepts a complete post', () => expect(validateHomework(ok)).toBeNull());
  it('needs a title and instructions, and a due date for homework only', () => {
    expect(validateHomework({ ...ok, title: ' ' })).toMatch(/title/);
    expect(validateHomework({ ...ok, description: '' })).toMatch(/instructions/);
    expect(validateHomework({ ...ok, dueDate: '' })).toMatch(/due date/);
    expect(validateHomework({ ...ok, kind: 'NOTE', dueDate: '' })).toBeNull();
  });
  it('checks the links', () => {
    expect(validateHomework({ ...ok, linksText: 'https://a.com\nnot a link' })).toMatch(/not a valid web link/);
    expect(validateHomework({ ...ok, linksText: Array.from({ length: 6 }, (_, i) => `https://a.com/${i}`).join('\n') })).toMatch(/at most 5/);
  });
});

describe('effectiveSection and effectiveSubject', () => {
  const options = [{ id: 's1' }, { id: 's2' }];
  it('uses the first section when nothing has been chosen, which was the original bug', () => {
    expect(effectiveSection('', options)).toBe('s1');
    expect(effectiveSection(undefined, options)).toBe('s1');
  });
  it('keeps a valid choice and replaces one that is no longer on offer', () => {
    expect(effectiveSection('s2', options)).toBe('s2');
    expect(effectiveSection('gone', options)).toBe('s1');
  });
  it('has no section while the options are still loading', () => {
    expect(effectiveSection('', [])).toBe('');
  });
  it('keeps a subject only if it is allowed for that section', () => {
    const allowed = [{ id: 'eng' }];
    expect(effectiveSubject('eng', allowed)).toBe('eng');
    expect(effectiveSubject('mat', allowed)).toBe('');
    expect(effectiveSubject('', allowed)).toBe('');
  });
});