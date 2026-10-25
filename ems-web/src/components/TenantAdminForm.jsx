import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { useListTenantsQuery } from '../services/tenantsApi';
import { useCreateTenantAdminMutation } from '../services/usersApi';
import { ErrorNotice, Field } from './ui';

const EMPTY = { tenantId: '', email: '', firstName: '', lastName: '' };

export default function TenantAdminForm() {
  const dispatch = useDispatch();
  const [form, setForm] = useState(EMPTY);
  const { data: tenants = [] } = useListTenantsQuery();
  const [invite, { isLoading, error }] = useCreateTenantAdminMutation();

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      const result = await invite(form).unwrap();
      setForm(EMPTY);
      dispatch(pushToast(result.invitationSent
        ? { text: `Invitation sent to ${result.user.email}` }
        : { tone: 'warn', text: 'Admin created, but the email could not be sent.' }));
    } catch { /* shown below */ }
  };

  return (
    <section className="card">
      <h2>Invite a school administrator</h2>
      <form onSubmit={onSubmit} className="form-grid">
        <Field label="School">
          <select name="tenantId" value={form.tenantId} onChange={onChange} required>
            <option value="" disabled>Select a school…</option>
            {tenants.filter((t) => t.status === 'ACTIVE').map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        </Field>
        <Field label="Email"><input type="email" name="email" value={form.email} onChange={onChange} required /></Field>
        <Field label="First name"><input name="firstName" value={form.firstName} onChange={onChange} required maxLength={100} /></Field>
        <Field label="Last name"><input name="lastName" value={form.lastName} onChange={onChange} required maxLength={100} /></Field>
        <button className="btn btn-primary" disabled={isLoading}>{isLoading ? 'Sending…' : 'Send invitation'}</button>
      </form>
      <ErrorNotice error={error} />
    </section>
  );
}