import { describe, expect, it } from 'vitest';
import { navFor } from './nav';
import { initials, statusTone } from './format';

const labels = (roles) => navFor(roles).map((i) => i.label);

describe('navFor', () => {
  it('shows platform admins only the Schools screen', () => {
    expect(labels(['PLATFORM_ADMIN'])).toEqual(['Schools']);
  });
  it('shows teachers people and the shared modules, but not Fees', () => {
    expect(labels(['TEACHER'])).toEqual(['Dashboard', 'People', 'Attendance', 'Network hub']);
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
});