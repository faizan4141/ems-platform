import { describe, expect, it } from 'vitest';
import { navFor } from './nav';
import { initials, statusTone } from './format';
import { parseSections } from './format';

const labels = (roles) => navFor(roles).map((i) => i.label);

describe('navFor', () => {
  it('shows platform admins only the Schools screen', () => {
    expect(labels(['PLATFORM_ADMIN'])).toEqual(['Schools']);
  });
  it('shows teachers their working screens, but not people, staff or fees', () => {
    expect(labels(['TEACHER'])).toEqual(['Dashboard', 'Students', 'Attendance', 'Timetable', 'Exams', 'Notices', 'Network hub']);
  });
  it('shows school admins people, staff and fees as well', () => {
    expect(labels(['TENANT_ADMIN'])).toEqual(expect.arrayContaining(['People', 'Staff', 'Fees', 'School setup']));
  });
  it('shows parents only Today and Notices', () => {
    expect(labels(['PARENT'])).toEqual(['Today', 'Notices']);
  });
  it('shows tenant admins Fees as well', () => {
    expect(labels(['TENANT_ADMIN'])).toContain('Fees');
  });
});

describe('format helpers', () => {
  it('builds initials', () => {
    expect(initials('Asha Verma')).toBe('AV');
    expect(initials('')).toBe('');
  });
  it('maps statuses to tones', () => {
    expect(statusTone.ACTIVE).toBe('ok');
    expect(statusTone.SUSPENDED).toBe('bad');
  });
  it('splits a comma-separated list of sections', () => {
    expect(parseSections('A, B ,, C')).toEqual(['A', 'B', 'C']);
    expect(parseSections('')).toEqual([]);
  });
  it('shows tenant admins the School setup screen, but not teachers', () => {
    expect(navFor(['TENANT_ADMIN']).map((i) => i.label)).toContain('School setup');
    expect(navFor(['TEACHER']).map((i) => i.label)).not.toContain('School setup');
  });
});