import { describe, expect, it } from 'vitest';
import { hasAnyRole, homePathFor } from './roles';

describe('homePathFor', () => {
  it('sends platform admins to schools', () => {
    expect(homePathFor(['PLATFORM_ADMIN'])).toBe('/tenants');
  });
  it('sends tenant admins and teachers to the dashboard, ignoring Keycloak default roles', () => {
    expect(homePathFor(['default-roles-ems', 'TENANT_ADMIN'])).toBe('/');
    expect(homePathFor(['TEACHER'])).toBe('/');
  });
  it('prefers the platform view when someone has both', () => {
    expect(homePathFor(['TENANT_ADMIN', 'PLATFORM_ADMIN'])).toBe('/tenants');
  });
  it('sends everyone else to the no-access page', () => {
    expect(homePathFor(['STUDENT'])).toBe('/no-access');
    expect(homePathFor([])).toBe('/no-access');
  });
});

describe('hasAnyRole', () => {
  it('matches when at least one role is present', () => {
    expect(hasAnyRole(['TEACHER'], ['TENANT_ADMIN', 'TEACHER'])).toBe(true);
    expect(hasAnyRole(['STUDENT'], ['TENANT_ADMIN', 'TEACHER'])).toBe(false);
  });
});