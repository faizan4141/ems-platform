import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { useAuth } from '../features/auth/useAuth';
import { fullName, placementLabel, splitName } from '../app/students';
import { statusTone } from '../app/format';
import { useListClassesQuery } from '../services/academicApi';
import { useGetStudentQuery, useUpdateStudentMutation } from '../services/studentsApi';
import { Chip, ErrorNotice, Field, PageHeader } from '../components/ui';
import { api } from '../services/api';
import { useCreateUserMutation } from '../services/usersApi';

function Placement({ student }) {
  const dispatch = useDispatch();
  const { data: classes = [] } = useListClassesQuery();
  const [update, { isLoading, error }] = useUpdateStudentMutation();
  const [sectionId, setSectionId] = useState(student.sectionId ?? '');

  const save = async (changes) => {
    try {
      await update({
        id: student.id, firstName: student.firstName, lastName: student.lastName,
        dateOfBirth: student.dateOfBirth, gender: student.gender,
        sectionId: student.sectionId, status: student.status, ...changes,
      }).unwrap();
      dispatch(pushToast({ text: 'Student updated' }));
    } catch { /* shown below */ }
  };

  const options = classes.flatMap((c) => c.sections.map((s) => ({ id: s.id, label: `${c.name} · ${s.name}` })));
  const changed = sectionId !== (student.sectionId ?? '');

  return (
    <section className="card">
      <h2>Class and status</h2>
      <div className="form-grid">
        <Field label="Class and section">
          <select value={sectionId} onChange={(e) => setSectionId(e.target.value)}>
            <option value="">Not assigned</option>
            {options.map((o) => <option key={o.id} value={o.id}>{o.label}</option>)}
          </select>
        </Field>
        <button className="btn btn-primary" disabled={!changed || isLoading}
                onClick={() => save({ sectionId: sectionId || null })}>Save placement</button>
        <button className="btn btn-secondary" disabled={isLoading}
                onClick={() => save({ status: student.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' })}>
          {student.status === 'ACTIVE' ? 'Mark inactive' : 'Mark active'}
        </button>
      </div>
      <ErrorNotice error={error} />
    </section>
  );
}

export default function StudentDetailPage() {
  const { id } = useParams();
  const canManage = useAuth().roles.includes('TENANT_ADMIN');
  const { data: student, isLoading, error } = useGetStudentQuery(id);
  const dispatch = useDispatch();
  const [createUser, { isLoading: inviting }] = useCreateUserMutation();

  const invite = async (guardian) => {
    const { first, last } = splitName(guardian.name);
    try {
      await createUser({ email: guardian.email, firstName: first, lastName: last, role: 'PARENT' }).unwrap();
      dispatch(pushToast({ text: `Invitation sent to ${guardian.email}` }));
      // the "Portal access" badge appears once the login event has been processed
      setTimeout(() => dispatch(api.util.invalidateTags(['Student'])), 2500);
    } catch (err) {
      dispatch(pushToast({ tone: 'error', text: err.message }));
    }
  };

  if (isLoading) return <div className="skeleton" style={{ width: '40%' }} />;
  if (error) return <><Link to="/students" className="btn-link">← Students</Link><ErrorNotice error={error} /></>;

  return (
    <>
      <Link to="/students" className="btn-link">← Students</Link>
      <div style={{ height: 12 }} />
      <PageHeader title={fullName(student)} subtitle={`Admission no. ${student.admissionNo}`}
        action={<Chip tone={statusTone[student.status]}>{student.status}</Chip>} />

      <section className="card">
        <h2>Details</h2>
        <dl className="detail-grid">
          <div><dt>Date of birth</dt><dd>{student.dateOfBirth}</dd></div>
          <div><dt>Gender</dt><dd>{student.gender ?? '—'}</dd></div>
          <div><dt>Admitted</dt><dd>{student.admissionDate}</dd></div>
          <div><dt>Class</dt><dd>{placementLabel(student)}</dd></div>
        </dl>
      </section>

      <section className="card">
        <h2>Guardians</h2>
        <ul className="checklist">
          {student.guardians.map((g) => (
            <li key={g.id}>
              <span>
                <strong>{g.name}</strong> <span className="muted">· {g.relationship}</span><br />
                <span className="muted">{g.phone}{g.email ? ` · ${g.email}` : ''}</span>
              </span>
              <span style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                {g.primary && <Chip tone="info">Primary contact</Chip>}
                {g.portalAccess ? <Chip tone="ok">Portal access</Chip>
                  : !g.email ? <span className="muted">No email on file</span>
                  : canManage && <button className="btn btn-secondary" disabled={inviting} onClick={() => invite(g)}>Invite to parent portal</button>}
              </span>
            </li>
          ))}
        </ul>
      </section>

      {canManage && <Placement student={student} />}
    </>
  );
}