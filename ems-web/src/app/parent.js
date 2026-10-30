import { STATUS_LABEL, STATUS_TONE } from './attendance';

export function todayView(status) {
  return status ? { label: STATUS_LABEL[status], tone: STATUS_TONE[status] } : { label: 'Not marked yet', tone: 'neutral' };
}

export function percentTone(percent) {
  if (percent == null) return 'neutral';
  if (percent >= 90) return 'ok';
  if (percent >= 75) return 'warn';
  return 'bad';
}

export const AUDIENCE_LABEL = { ALL: 'Everyone', PARENTS: 'Parents', STAFF: 'Staff' };

// ISO dates are calendar days, not moments in time, so format them in UTC to avoid off-by-one shifts.
export function shortDate(iso) {
  return new Date(`${iso}T00:00:00Z`).toLocaleDateString('en-GB',
    { weekday: 'short', day: 'numeric', month: 'short', timeZone: 'UTC' });
}