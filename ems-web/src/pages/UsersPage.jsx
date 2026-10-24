import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { usersApi } from '../api/users';
import { useAuth } from '../auth/AuthProvider';
import ErrorMessage from '../components/ErrorMessage';

const ROLES = ['TEACHER', 'STUDENT', 'PARENT', 'STAFF', 'ADMIN'];
const EMPTY_FORM = { email: '', firstName: '', lastName: '', role: 'TEACHER' };

export default function UsersPage() {
  const auth = useAuth();
  const canCreate = auth.roles.includes('TENANT_ADMIN');
  const queryClient = useQueryClient();
  const [form, setForm] = useState(EMPTY_FORM);

  const users = useQuery({ queryKey: ['users'], queryFn: usersApi.list });

  const createUser = useMutation({
    mutationFn: usersApi.create,
    onSuccess: () => {
      setForm(EMPTY_FORM);
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
  });

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });
  const onSubmit = (e) => {
    e.preventDefault();
    createUser.mutate(form);
  };

  return (
    <>
      <h1>Users</h1>

      {canCreate && (
        <section className="card">
          <h2>Add a user</h2>
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
        </section>
      )}

      <section className="card">
        <h2>People in your school</h2>
        <ErrorMessage error={users.error} />
        {users.isPending && <p>Loading…</p>}
        {users.data?.length === 0 && <p>No users yet.</p>}
        {users.data?.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th></tr>
              </thead>
              <tbody>
                {users.data.map((u) => (
                  <tr key={u.id}>
                    <td>{u.firstName} {u.lastName}</td>
                    <td>{u.email}</td>
                    <td>{u.role}</td>
                    <td><span className={`badge ${u.status.toLowerCase()}`}>{u.status}</span></td>
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