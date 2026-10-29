export const statusTone = { ACTIVE: 'ok', SUSPENDED: 'bad', EXPIRED: 'bad', INACTIVE: 'bad' };

export function initials(name = '') {
  return name.split(' ').filter(Boolean).slice(0, 2).map((w) => w[0].toUpperCase()).join('');
}

export function parseSections(text = '') {
  return text.split(',').map((s) => s.trim()).filter(Boolean);
}