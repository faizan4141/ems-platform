import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { useAuth } from '../features/auth/useAuth';
import { statusTone } from '../app/format';
import { useCreateUserMutation, useListUsersQuery, useResendInvitationMutation } from '../services/usersApi';
import { Avatar, Chip, EmptyState, ErrorNotice, Field, PageHeader, SkeletonRows } from '../components/ui';

const ROLES = ['TEACHER', 'STUDENT', 'PARENT', 'STAFF', 'ADMIN'];
const EMPTY = { email: '', firstName: '', lastName: '', role: 'TEACHER' };

export default function UsersPage() {
  const dispatch = useDispatch();
  const canManage = useAuth().roles.includes('TENANT_ADMIN');
  const [form, setForm] = useState(EMPTY);
  const { data: users, isLoading, error: listError } = useListUsersQuery();
  const [createUser, { isLoading: creating, error: createError }] = useCreateUserMutation();
  const [resend, { isLoading: resending, originalArgs: resendingId }] = useResendInvitationMutation();

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      const result = await createUser(form).unwrap();
      setForm(EMPTY);
      dispatch(pushToast(result.invitationSent
        ? { text: `Invitation sent to ${result.user.email}` }
        : { tone: 'warn', text: `${result.user.email} was created, but the email failed. Use "Resend invite".` }));
    } catch { /* shown below */ }
  };

  const onResend = async (user) => {
    try {
      await resend(user.id).unwrap();
      dispatch(pushToast({ text: `Invitation sent to ${user.email}` }));
    } catch (err) {
      dispatch(pushToast({ tone: 'error', text: err.message }));
    }
  };

  return (
    <>
      <PageHeader title="People" subtitle="Teachers, students, parents and staff in your school." />

      {canManage && (
        <section className="card">
          <h2>Add a person</h2>
          <p className="muted" style={{ marginTop: 0 }}>They'll get an email with a link to set their own password.</p>
          <form onSubmit={onSubmit} className="form-grid">
            <Field label="Email"><input type="email" name="email" value={form.email} onChange={onChange} required /></Field>
            <Field label="First name"><input name="firstName" value={form.firstName} onChange={onChange} required maxLength={100} /></Field>
            <Field label="Last name"><input name="lastName" value={form.lastName} onChange={onChange} required maxLength={100} /></Field>
            <Field label="Role">
              <select name="role" value={form.role} onChange={onChange}>
                {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
              </select>
            </Field>
            <button className="btn btn-primary" disabled={creating}>{creating ? 'Saving…' : 'Create'}</button>
          </form>
          <ErrorNotice error={createError} />
        </section>
      )}

      <section className="card">
        <h2>Everyone</h2>
        <ErrorNotice error={listError} />
        {users?.length === 0 && <EmptyState title="No one here yet" text="Add your first teacher to get started." />}
        {(isLoading || users?.length > 0) && (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Login</th>{canManage && <th></th>}</tr></thead>
              <tbody>
                {isLoading && <SkeletonRows cols={canManage ? 6 : 5} />}
                {users?.map((u) => (
                  <tr key={u.id}>
                    <td><div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <Avatar name={`${u.firstName} ${u.lastName}`} /><strong>{u.firstName} {u.lastName}</strong></div></td>
                    <td className="muted">{u.email}</td>
                    <td>{u.role}</td>
                    <td><Chip tone={statusTone[u.status]}>{u.status}</Chip></td>
                    <td>{u.hasLogin ? <Chip tone="ok">Created</Chip> : <Chip tone="warn">Not set up</Chip>}</td>
                    {canManage && (
                      <td>
                        <button className="btn btn-secondary" disabled={resending} onClick={() => onResend(u)}>
                          {resending && resendingId === u.id ? 'Sending…' : u.hasLogin ? 'Resend invite' : 'Set up login'}
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