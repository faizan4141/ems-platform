import { describe, expect, it } from 'vitest';
import { FEE_STATUS, METHODS, NEEDS_REFERENCE, formatMoney, methodLabel, validateAmount } from './fees';

describe('formatMoney', () => {
  it('formats amounts with two decimals and the currency', () => {
    expect(formatMoney(12500, 'INR', 'en-IN')).toMatch(/12,500\.00/);
  });
  it('uses Indian digit grouping for en-IN', () => {
    expect(formatMoney(125000, 'INR', 'en-IN')).toContain('1,25,000.00');
  });
  it('treats a missing amount as zero', () => {
    expect(formatMoney(undefined, 'INR', 'en-IN')).toMatch(/0\.00/);
  });
});

describe('validateAmount', () => {
  it('accepts a normal amount up to the balance', () => {
    expect(validateAmount('5000', 12000)).toBeNull();
    expect(validateAmount('12000.00', 12000)).toBeNull();
  });
  it('rejects empty, zero and negative amounts', () => {
    expect(validateAmount('', 100)).toMatch(/greater than zero/);
    expect(validateAmount('0', 100)).toMatch(/greater than zero/);
    expect(validateAmount('-5', 100)).toMatch(/greater than zero/);
  });
  it('rejects more than two decimals and more than the balance', () => {
    expect(validateAmount('10.999', 100)).toMatch(/two decimal/);
    expect(validateAmount('100.01', 100)).toMatch(/more than/);
  });
});

describe('payment methods and statuses', () => {
  it('requires a reference for traceable methods only', () => {
    expect(NEEDS_REFERENCE).toEqual(expect.arrayContaining(['CHEQUE', 'UPI', 'BANK_TRANSFER']));
    expect(NEEDS_REFERENCE).not.toContain('CASH');
  });
  it('labels methods and every status', () => {
    expect(methodLabel('BANK_TRANSFER')).toBe('Bank transfer');
    expect(Object.keys(FEE_STATUS).sort()).toEqual(['DUE', 'OVERDUE', 'PAID', 'PARTIAL']);
    expect(METHODS.map((m) => m.value)).toContain('CASH');
  });
  it('labels online payments, which admins cannot record by hand', () => {
    expect(methodLabel('ONLINE')).toBe('Online');
    expect(METHODS.map((m) => m.value)).not.toContain('ONLINE');
  });
});