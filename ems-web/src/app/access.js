// Admins choose from every section; teachers only from the ones they are assigned to.
export function sectionOptions({ isAdmin, classes = [], teaching }) {
  if (isAdmin) {
    return classes.flatMap((c) => c.sections.map((s) => ({
      id: s.id, classId: c.id, sectionName: s.name, label: `${c.name} · ${s.name}`,
    })));
  }
  return (teaching?.sections ?? []).map((s) => ({
    id: s.sectionId, classId: s.classId, sectionName: s.sectionName, label: `${s.className} · ${s.sectionName}`,
  }));
}

export const classIdsOf = (options) => [...new Set(options.map((o) => o.classId))];

export function describeAssignment(a) {
  const where = `${a.className} · ${a.sectionName}`;
  return a.classTeacher ? `Class teacher, ${where}` : `${a.subjectName}, ${where}`;
}