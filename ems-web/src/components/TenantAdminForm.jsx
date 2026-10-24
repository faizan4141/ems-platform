import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { usersApi } from '../api/users';
import ErrorMessage from './ErrorMessage';

const EMPTY = { tenantId: '', email: '', firstName: '', lastName: '' };

export default function TenantAdminForm({ tenants }) {
  const [form, setForm] = useState(EMPTY);
  const [notice, setNotice] = useState(null);

  const invite = useMutation({
    mutationFn: usersApi.createTenantAdmin,
    onSuccess: (result) => {
      setForm(EMPTY);
      setNotice(
        result.invitationSent
          ? { warn: false, text: `Invitation sent to ${result.user.email}.` }
          : { warn: true, text: 'The admin was created, but the email could not be sent. They can be re-invited from the school’s Users page once they can sign in, or ask us to resend.' },
      );
    },
  });

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  const onSubmit = (e) => {
    e.preventDefault();
    setNotice(null);
    invite.mutate(form);
  };

  const activeTenants = tenants.filter((t) => t.status === 'ACTIVE');

  return (
    <section className="card">
      <h2>Invite a school administrator</h2>
      <form onSubmit={onSubmit} className="grid">
        <label>
          School
          <select name="tenantId" value={form.tenantId} onChange={onChange} required>
            <option value="" disabled>Select a school…</option>
            {activeTenants.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
          </select>
        </label>
        <label>
          Email
          <input type="email" name="email" value={form.email} onChange={onChange} required />
        </label>
        <label>
          First name
          <input name="firstName" value={form.firstName} onChange={onChange} required maxLength={100} />
        </label>
        <label>
          Last name
          <input name="lastName" value={form.lastName} onChange={onChange} required maxLength={100} />
        </label>
        <div className="actions">
          <button type="submit" disabled={invite.isPending}>
            {invite.isPending ? 'Sending…' : 'Send invitation'}
          </button>
        </div>
      </form>
      <ErrorMessage error={invite.error} />
      {notice && <div className={`notice ${notice.warn ? 'warn' : ''}`}>{notice.text}</div>}
    </section>
  );
}