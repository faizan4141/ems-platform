export const ATTEMPT = {
  PENDING: { label: 'Waiting for payment', tone: 'warn' },
  SUCCEEDED: { label: 'Paid', tone: 'ok' },
  FAILED: { label: 'Failed', tone: 'bad' },
  REVIEW: { label: 'Needs review', tone: 'bad' },
};

export const payableCharges = (ledger) => ledger.charges.filter((c) => c.balance > 0);

// Sum in whole cents so floating-point errors can never show up in front of a parent.
export function selectedTotal(charges, selectedIds) {
  const cents = charges
    .filter((c) => selectedIds.includes(c.id))
    .reduce((sum, c) => sum + Math.round(c.balance * 100), 0);
  return cents / 100;
}

export function returnMessage(status) {
  switch (status) {
    case 'SUCCEEDED': return { tone: 'ok', text: 'Payment received. Thank you! Your receipts are listed below.' };
    case 'FAILED': return { tone: 'bad', text: 'The payment did not go through, and you have not been charged. You can try again.' };
    case 'REVIEW': return { tone: 'warn', text: 'We received your payment, but the school needs to check it. They will contact you.' };
    default: return { tone: 'warn', text: 'We are confirming your payment. This usually takes a few seconds.' };
  }
}

// Only ever follow web links, never anything like javascript: URLs.
export function safeRedirectUrl(url) {
  try {
    const parsed = new URL(url);
    return ['http:', 'https:'].includes(parsed.protocol) ? parsed.toString() : null;
  } catch {
    return null;
  }
}