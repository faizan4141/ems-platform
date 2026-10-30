import { describe, expect, it } from 'vitest';
import { CSV_COLUMNS, csvTemplate, fullName, placementLabel, splitName } from './students';

describe('csvTemplate', () => {
  it('starts with the header the server expects', () => {
    const [header, example] = csvTemplate().split('\n');
    expect(header).toBe(CSV_COLUMNS.join(','));
    expect(example.split(',')).toHaveLength(CSV_COLUMNS.length);
  });
  it('includes every column the backend requires', () => {
    ['admission_no', 'first_name', 'last_name', 'date_of_birth', 'guardian_name', 'guardian_phone']
      .forEach((c) => expect(CSV_COLUMNS).toContain(c));
  });
});

describe('student labels', () => {
  it('builds names and placements', () => {
    expect(fullName({ firstName: 'Aarav', lastName: 'Sharma' })).toBe('Aarav Sharma');
    expect(placementLabel({ className: 'Class 8', sectionName: 'A' })).toBe('Class 8 · A');
    expect(placementLabel({})).toBe('Not assigned');
  });

describe('splitName', () => {
  it('splits a full name at the last space', () => {
    expect(splitName('Raj Kumar Sharma')).toEqual({ first: 'Raj Kumar', last: 'Sharma' });
  });
  it('repeats a single name, because the platform needs both fields', () => {
    expect(splitName('Madonna')).toEqual({ first: 'Madonna', last: 'Madonna' });
    expect(splitName('')).toEqual({ first: '', last: '' });
  });
});
});