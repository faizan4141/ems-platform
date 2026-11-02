import { describe, expect, it } from 'vitest';
import { classIdsOf, describeAssignment, sectionOptions } from './access';

const classes = [{ id: 'c8', name: 'Class 8', sections: [{ id: 's8a', name: 'A' }, { id: 's8b', name: 'B' }] }];
const teaching = { sections: [{ sectionId: 's8a', classId: 'c8', className: 'Class 8', sectionName: 'A' }] };

describe('sectionOptions', () => {
  it('gives admins every section of every class', () => {
    expect(sectionOptions({ isAdmin: true, classes }).map((o) => o.label)).toEqual(['Class 8 · A', 'Class 8 · B']);
  });
  it('gives teachers only the sections they are assigned to', () => {
    const options = sectionOptions({ isAdmin: false, classes, teaching });
    expect(options).toEqual([{ id: 's8a', classId: 'c8', sectionName: 'A', label: 'Class 8 · A' }]);
  });
  it('gives a teacher without assignments nothing', () => {
    expect(sectionOptions({ isAdmin: false, classes, teaching: { sections: [] } })).toEqual([]);
    expect(sectionOptions({ isAdmin: false, classes })).toEqual([]);
  });
});

describe('classIdsOf and describeAssignment', () => {
  it('lists each class once', () => {
    expect(classIdsOf([{ classId: 'c8' }, { classId: 'c8' }, { classId: 'c9' }])).toEqual(['c8', 'c9']);
  });
  it('describes both kinds of assignment', () => {
    const base = { className: 'Class 8', sectionName: 'A' };
    expect(describeAssignment({ ...base, classTeacher: true })).toBe('Class teacher, Class 8 · A');
    expect(describeAssignment({ ...base, classTeacher: false, subjectName: 'English' })).toBe('English, Class 8 · A');
  });
});