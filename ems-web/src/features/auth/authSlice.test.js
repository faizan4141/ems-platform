import { describe, expect, it } from 'vitest';
import reducer, { authFailed, authReady, profileFromToken } from './authSlice';

describe('profileFromToken', () => {
  it('reads roles and tenant from token claims', () => {
    const p = profileFromToken({ name: 'Asha Verma', given_name: 'Asha', email: 'a@b.com',
      realm_access: { roles: ['TENANT_ADMIN'] }, tenant_id: 't-1' });
    expect(p).toMatchObject({ firstName: 'Asha', roles: ['TENANT_ADMIN'], tenantId: 't-1' });
  });
  it('copes with an empty token', () => {
    expect(profileFromToken(undefined)).toMatchObject({ roles: [], tenantId: null, name: '' });
  });
});

describe('authSlice', () => {
  it('becomes ready with the profile', () => {
    const state = reducer(undefined, authReady({ name: 'A', roles: ['TEACHER'] }));
    expect(state.status).toBe('ready');
    expect(state.roles).toEqual(['TEACHER']);
  });
  it('records a failure', () => {
    expect(reducer(undefined, authFailed()).status).toBe('error');
  });
});