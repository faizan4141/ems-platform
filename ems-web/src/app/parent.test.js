import { describe, expect, it } from 'vitest';
import { AUDIENCE_LABEL, percentTone, shortDate, todayView } from './parent';

describe('parent helpers', () => {
  it('shows an unmarked day as not marked yet', () => {
    expect(todayView(null)).toEqual({ label: 'Not marked yet', tone: 'neutral' });
    expect(todayView('ABSENT')).toEqual({ label: 'Absent', tone: 'bad' });
  });

  it('colors the attendance percentage', () => {
    expect(percentTone(95)).toBe('ok');
    expect(percentTone(80)).toBe('warn');
    expect(percentTone(60)).toBe('bad');
    expect(percentTone(null)).toBe('neutral');
  });

  it('formats a calendar date without shifting the day', () => {
    const text = shortDate('2026-10-30');
    expect(text).toContain('30');
    expect(text).toContain('Oct');
  });

  it('labels notice audiences', () => {
    expect(AUDIENCE_LABEL.PARENTS).toBe('Parents');
  });
});