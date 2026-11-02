import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { describeAssignment } from '../app/access';
import { useSectionOptions } from '../app/useSectionOptions';
import { useListSubjectsQuery } from '../services/examsApi';
import {
  useAssignTeacherMutation, useListStaffQuery, useUnassignTeacherMutation, useUpdateStaffMutation,
} from '../services/staffApi';
import { Chip, EmptyState, ErrorNotice, Field, PageHeader } from '../components/ui';

function AssignForm({ staffId }) {
  const dispatch = useDispatch();
  const { options } = useSectionOptions();
  const { data: subjects = [] } = useListSubjectsQuery();
  const [assign, { isLoading, error, reset }] = useAssignTeacherMutation();
  const [sectionId, setSectionId] = useState('');
  const [kind, setKind] = useState('SUBJECT');
  const [subjectId, setSubjectId] = useState('');

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      await assign({
        staffId, sectionId, classTeacher: kind === 'CLASS', subjectId: kind === 'CLASS' ? null : subjectId,
      }).unwrap();
      dispatch(pushToast({ text: 'Assignment added' }));
      setSectionId('');
      setSubjectId('');
      reset();
    } catch { /* shown below */ }
  };

  return (
    <form onSubmit={onSubmit} className="form-grid" style={{ marginTop: 12 }}>
      <Field label="Class and section">
        <select value={sectionId} onChange={(e) => setSectionId(e.target.value)} required>
          <option value="" disabled>Select…</option>
          {options.map((o) => <option key={o.id} value={o.id}>{o.label}</option>)}
        </select>
      </Field>
      <Field label="Role">
        <select value={kind} onChange={(e) => setKind(e.target.value)}>
          <option value="SUBJECT">Subject teacher</option>
          <option value="CLASS">Class teacher</option>
        </select>
      </Field>
      {kind === 'SUBJECT' && (
        <Field label="Subject">
          <select value={subjectId} onChange={(e) => setSubjectId(e.target.value)} required>
            <option value="" disabled>Select…</option>
            {subjects.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </Field>
      )}
      <button className="btn btn-primary" disabled={isLoading}>{isLoading ? 'Adding…' : 'Assign'}</button>
      <div style={{ gridColumn: '1 / -1' }}><ErrorNotice error={error} /></div>
    </form>
  );
}

function StaffCard({ member }) {
  const dispatch = useDispatch();
  const [updateStaff, { isLoading }] = useUpdateStaffMutation();
  const [unassign] = useUnassignTeacherMutation();
  const [designation, setDesignation] = useState(member.designation ?? '');

  const save = async (patch, message) => {
    try {
      await updateStaff({ id: member.id, designation: designation.trim() || null, active: member.active, ...patch }).unwrap();
      dispatch(pushToast({ text: message }));
    } catch (err) {
      dispatch(pushToast({ tone: 'error', text: err.message }));
    }
  };

  const remove = async (assignment) => {
    try {
      await unassign(assignment.id).unwrap();
    } catch (err) {
      dispatch(pushToast({ tone: 'error', text: err.message }));
    }
  };

  return (
    <section className="card">
      <div className="page-header" style={{ marginBottom: 12 }}>
        <div>
          <h2 style={{ margin: 0 }}>{member.name} {!member.active && <Chip tone="bad">Inactive</Chip>}</h2>
          <span className="muted">{member.email}</span>
        </div>
        <button className="btn btn-secondary" disabled={isLoading}
                onClick={() => save({ active: !member.active }, member.active ? 'Teacher deactivated' : 'Teacher activated')}>
          {member.active ? 'Deactivate' : 'Activate'}
        </button>
      </div>

      <div className="toolbar">
        <Field label="Designation (optional)">
          <input value={designation} onChange={(e) => setDesignation(e.target.value)} maxLength={100} placeholder="Head of Mathematics" />
        </Field>
        <button className="btn btn-secondary" disabled={isLoading || designation === (member.designation ?? '')}
                onClick={() => save({}, 'Saved')}>Save</button>
      </div>

      <h2 style={{ fontSize: '0.95rem' }}>Teaching</h2>
      {member.assignments.length === 0
        ? <p className="muted">Not assigned to any class yet, so this teacher cannot see any students.</p>
        : (
          <ul className="checklist">
            {member.assignments.map((a) => (
              <li key={a.id}>
                <span>{describeAssignment(a)}</span>
                <span className="row-actions">
                  <Chip tone={a.classTeacher ? 'info' : 'neutral'}>{a.classTeacher ? 'Class teacher' : 'Subject'}</Chip>
                  <button className="btn btn-secondary" onClick={() => remove(a)}>Remove</button>
                </span>
              </li>
            ))}
          </ul>
        )}
      {member.active && <AssignForm staffId={member.id} />}
    </section>
  );
}

export default function StaffPage() {
  const { data: staff, isLoading, error } = useListStaffQuery();
  return (
    <>
      <PageHeader title="Staff" subtitle="Assign teachers to the classes and subjects they teach. They see only what they are assigned to." />
      <ErrorNotice error={error} />
      {isLoading && <div className="skeleton" style={{ width: '40%' }} />}
      {staff?.length === 0 && (
        <section className="card">
          <EmptyState title="No teachers yet"
            text="Add a person with the Teacher role in People. They appear here once their invitation has been sent. For teachers added earlier, use “Resend invite”." />
        </section>
      )}
      {staff?.map((m) => <StaffCard key={m.id} member={m} />)}
    </>
  );
}