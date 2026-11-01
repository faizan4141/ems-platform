import { describe, expect, it } from 'vitest';
import { ATTEMPT, payableCharges, returnMessage, safeRedirectUrl, selectedTotal } from './onlinePay';

const charges = [
  { id: 'a', balance: 100.1 },
  { id: 'b', balance: 0.2 },
  { id: 'c', balance: 0 },
];

describe('payableCharges and selectedTotal', () => {
  it('offers only fees with something left to pay', () => {
    expect(payableCharges({ charges }).map((c) => c.id)).toEqual(['a', 'b']);
  });
  it('adds in cents, so 100.10 + 0.20 is exactly 100.30', () => {
    expect(selectedTotal(charges, ['a', 'b'])).toBe(100.3);
  });
  it('counts only the selected fees', () => {
    expect(selectedTotal(charges, ['b'])).toBe(0.2);
    expect(selectedTotal(charges, [])).toBe(0);
  });
});

describe('returnMessage', () => {
  it('has a calm, honest message for every outcome', () => {
    expect(returnMessage('SUCCEEDED').tone).toBe('ok');
    expect(returnMessage('FAILED').text).toMatch(/not been charged/);
    expect(returnMessage('REVIEW').text).toMatch(/school needs to check/);
    expect(returnMessage('PENDING').text).toMatch(/confirming/);
  });
  it('labels every attempt status', () => {
    expect(Object.keys(ATTEMPT).sort()).toEqual(['FAILED', 'PENDING', 'REVIEW', 'SUCCEEDED']);
  });
});

describe('safeRedirectUrl', () => {
  it('allows web links only', () => {
    expect(safeRedirectUrl('https://pay.example.com/c/1')).toBe('https://pay.example.com/c/1');
    expect(safeRedirectUrl('javascript:alert(1)')).toBeNull();
    expect(safeRedirectUrl('not a url')).toBeNull();
    expect(safeRedirectUrl(undefined)).toBeNull();
  });
});