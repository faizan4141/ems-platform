import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '../api/users';
import { useAuth } from '../auth/AuthProvider';
import ErrorMessage from '../components/ErrorMessage';

const ROLES = ['TEACHER', 'STUDENT', 'PARENT', 'STAFF', 'ADMIN'];
const EMPTY_FORM = { email: '', firstName: '', lastName: '', role: 'TEACHER' };

export default function UsersPage() {
  const auth = useAuth();
  const canManage = auth.roles.includes('TENANT_ADMIN');
  const queryClient = useQueryClient();
  const [form, setForm] = useState(EMPTY_FORM);
  const [notice, setNotice] = useState(null);

  const users = useQuery({ queryKey: ['users'], queryFn: usersApi.list });
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['users'] });

  const createUser = useMutation({
    mutationFn: usersApi.create,
    onSuccess: (result) => {
      setForm(EMPTY_FORM);
      setNotice(
        result.invitationSent
          ? { type: 'ok', text: `Invitation sent to ${result.user.email}.` }
          : {
              type: 'warn',
              text: `${result.user.email} was created, but the invitation email could not be sent. Use "Resend invite" in the list.`,
            },
      );
      refresh();
    },
  });

  const invite = useMutation({
    mutationFn: usersApi.resendInvitation,
    onSuccess: () => {
      setNotice({ type: 'ok', text: 'Invitation sent.' });
      refresh();
    },
  });

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  const onSubmit = (e) => {
    e.preventDefault();
    setNotice(null);
    createUser.mutate(form);
  };

  return (
    <>
      <h1>Users</h1>

      {canManage && (
        <section className="card">
          <h2>Add a user</h2>
          <p style={{ marginTop: 0, color: 'var(--muted)' }}>
            They'll receive an email with a link to set their own password.
          </p>
          <form onSubmit={onSubmit} className="grid">
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
            <label>
              Role
              <select name="role" value={form.role} onChange={onChange}>
                {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
              </select>
            </label>
            <div className="actions">
              <button type="submit" disabled={createUser.isPending}>
                {createUser.isPending ? 'Saving…' : 'Create user'}
              </button>
            </div>
          </form>
          <ErrorMessage error={createUser.error} />
          {notice && <div className={`notice ${notice.type === 'warn' ? 'warn' : ''}`}>{notice.text}</div>}
        </section>
      )}

      <section className="card">
        <h2>People in your school</h2>
        <ErrorMessage error={users.error || invite.error} />
        {users.isPending && <p>Loading…</p>}
        {users.data?.length === 0 && <p>No users yet.</p>}
        {users.data?.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Login</th>
                  {canManage && <th></th>}
                </tr>
              </thead>
              <tbody>
                {users.data.map((u) => (
                  <tr key={u.id}>
                    <td>{u.firstName} {u.lastName}</td>
                    <td>{u.email}</td>
                    <td>{u.role}</td>
                    <td><span className={`badge ${u.status.toLowerCase()}`}>{u.status}</span></td>
                    <td>{u.hasLogin ? 'Created' : 'Not set up'}</td>
                    {canManage && (
                      <td>
                        <button className="secondary" disabled={invite.isPending}
                                onClick={() => { setNotice(null); invite.mutate(u.id); }}>
                          {invite.isPending && invite.variables === u.id
                            ? 'Sending…'
                            : u.hasLogin ? 'Resend invite' : 'Set up login'}
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}