import { describe, expect, it } from 'vitest';
import { friendlyMessage } from './errors';

describe('friendlyMessage', () => {
  it('shows the server detail for conflicts', () => {
    expect(friendlyMessage(409, 'Tenant code already exists: abc')).toBe('Tenant code already exists: abc');
  });

  it('explains rate limiting', () => {
    expect(friendlyMessage(429)).toMatch(/too many requests/i);
  });

  it('explains outages', () => {
    expect(friendlyMessage(503)).toMatch(/temporarily unavailable/i);
  });

  it('never leaks server internals on 500', () => {
    expect(friendlyMessage(500, 'NullPointerException at TenantService')).not.toContain('NullPointer');
  });

  it('falls back to a generic message for a 400 without detail', () => {
    expect(friendlyMessage(400)).toMatch(/could not be completed/i);
  });

  it('explains a failing upstream service', () => {
    expect(friendlyMessage(502)).toMatch(/try again/i);
  });

  it('explains a network failure', () => {
    expect(friendlyMessage(0)).toMatch(/cannot reach the server/i);
  });

});