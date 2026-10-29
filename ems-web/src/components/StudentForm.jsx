import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { useCreateStudentMutation } from '../services/studentsApi';
import { ErrorNotice, Field } from './ui';

const EMPTY = {
  admissionNo: '', firstName: '', lastName: '', dateOfBirth: '', gender: '', sectionId: '',
  guardianName: '', relationship: 'FATHER', guardianPhone: '', guardianEmail: '',
};

export default function StudentForm({ sectionOptions, onDone }) {
  const dispatch = useDispatch();
  const [form, setForm] = useState(EMPTY);
  const [createStudent, { isLoading, error }] = useCreateStudentMutation();
  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      await createStudent({
        admissionNo: form.admissionNo, firstName: form.firstName, lastName: form.lastName,
        dateOfBirth: form.dateOfBirth, gender: form.gender || null, sectionId: form.sectionId || null,
        guardians: [{
          name: form.guardianName, relationship: form.relationship, phone: form.guardianPhone,
          email: form.guardianEmail || null, primary: true,
        }],
      }).unwrap();
      setForm(EMPTY);
      dispatch(pushToast({ text: 'Student added' }));
      onDone?.();
    } catch { /* shown below */ }
  };

  return (
    <form onSubmit={onSubmit} className="form-grid">
      <Field label="Admission number"><input name="admissionNo" value={form.admissionNo} onChange={onChange} required maxLength={30} /></Field>
      <Field label="First name"><input name="firstName" value={form.firstName} onChange={onChange} required maxLength={100} /></Field>
      <Field label="Last name"><input name="lastName" value={form.lastName} onChange={onChange} required maxLength={100} /></Field>
      <Field label="Date of birth"><input type="date" name="dateOfBirth" value={form.dateOfBirth} onChange={onChange} required /></Field>
      <Field label="Gender">
        <select name="gender" value={form.gender} onChange={onChange}>
          <option value="">Prefer not to say</option>
          <option value="MALE">Male</option>
          <option value="FEMALE">Female</option>
          <option value="OTHER">Other</option>
        </select>
      </Field>
      <Field label="Class and section">
        <select name="sectionId" value={form.sectionId} onChange={onChange}>
          <option value="">Assign later</option>
          {sectionOptions.map((o) => <option key={o.id} value={o.id}>{o.label}</option>)}
        </select>
      </Field>
      <Field label="Guardian name"><input name="guardianName" value={form.guardianName} onChange={onChange} required maxLength={200} /></Field>
      <Field label="Relationship">
        <select name="relationship" value={form.relationship} onChange={onChange}>
          {['FATHER', 'MOTHER', 'GUARDIAN', 'OTHER'].map((r) => <option key={r} value={r}>{r}</option>)}
        </select>
      </Field>
      <Field label="Guardian phone"><input name="guardianPhone" value={form.guardianPhone} onChange={onChange} required inputMode="tel" /></Field>
      <Field label="Guardian email (optional)"><input type="email" name="guardianEmail" value={form.guardianEmail} onChange={onChange} /></Field>
      <button className="btn btn-primary" disabled={isLoading}>{isLoading ? 'Saving…' : 'Add student'}</button>
      <div style={{ gridColumn: '1 / -1' }}><ErrorNotice error={error} /></div>
    </form>
  );
}