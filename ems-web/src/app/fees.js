export const METHODS = [
  { value: 'CASH', label: 'Cash' },
  { value: 'UPI', label: 'UPI' },
  { value: 'CHEQUE', label: 'Cheque' },
  { value: 'BANK_TRANSFER', label: 'Bank transfer' },
  { value: 'CARD', label: 'Card' },
  { value: 'OTHER', label: 'Other' },
];

export const NEEDS_REFERENCE = ['CHEQUE', 'UPI', 'BANK_TRANSFER'];

export const methodLabel = (method) =>
  method === 'ONLINE' ? 'Online' : METHODS.find((m) => m.value === method)?.label ?? method;

export const FEE_STATUS = {
  PAID: { label: 'Paid', tone: 'ok' },
  PARTIAL: { label: 'Part paid', tone: 'info' },
  DUE: { label: 'Due', tone: 'neutral' },
  OVERDUE: { label: 'Overdue', tone: 'bad' },
};

export function formatMoney(amount, currency = 'INR', locale) {
  return new Intl.NumberFormat(locale, { style: 'currency', currency }).format(amount ?? 0);
}

// One key per form submission: a retry or double click then returns the same receipt.
export const newKey = () => crypto.randomUUID();

export function validateAmount(text, balance) {
  const value = Number(text);
  if (!text || Number.isNaN(value) || value <= 0) return 'Enter an amount greater than zero';
  if (!/^\d+(\.\d{1,2})?$/.test(String(text).trim())) return 'Use at most two decimal places';
  if (value > balance + 1e-9) return 'The amount is more than the remaining balance';
  return null;
}