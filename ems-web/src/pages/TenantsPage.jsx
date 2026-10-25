import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { pushToast } from '../features/ui/uiSlice';
import { statusTone } from '../app/format';
import { useCreateTenantMutation, useListTenantsQuery, useUpdateTenantStatusMutation } from '../services/tenantsApi';
import TenantAdminForm from '../components/TenantAdminForm';
import { Chip, EmptyState, ErrorNotice, Field, PageHeader, SkeletonRows } from '../components/ui';

const ORG_TYPES = ['SCHOOL', 'COLLEGE', 'UNIVERSITY', 'COACHING_INSTITUTE'];
const EMPTY = { code: '', name: '', orgType: 'SCHOOL', contractStartDate: '', contractEndDate: '' };

export default function TenantsPage() {
  const dispatch = useDispatch();
  const [form, setForm] = useState(EMPTY);
  const { data: tenants, isLoading, error: listError } = useListTenantsQuery();
  const [createTenant, { isLoading: creating, error: createError }] = useCreateTenantMutation();
  const [updateStatus, { isLoading: changing }] = useUpdateTenantStatusMutation();

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    try {
      const created = await createTenant(form).unwrap();
      setForm(EMPTY);
      dispatch(pushToast({ text: `${created.name} created` }));
    } catch { /* shown below */ }
  };

  const toggle = async (tenant) => {
    const suspending = tenant.status === 'ACTIVE';
    if (suspending && !window.confirm(`Suspend ${tenant.name}? Its users will be blocked.`)) return;
    try {
      await updateStatus({ id: tenant.id, status: suspending ? 'SUSPENDED' : 'ACTIVE' }).unwrap();
      dispatch(pushToast({ text: `${tenant.name} ${suspending ? 'suspended' : 'activated'}` }));
    } catch (err) {
      dispatch(pushToast({ tone: 'error', text: err.message }));
    }
  };

  return (
    <>
      <PageHeader title="Schools" subtitle="Every organization on the platform." />

      <section className="card">
        <h2>Add a school</h2>
        <form onSubmit={onSubmit} className="form-grid">
          <Field label="Code">
            <input name="code" value={form.code} onChange={onChange} required pattern="[a-z0-9\-]{3,50}"
                   title="3-50 characters: lowercase letters, digits, hyphens" />
          </Field>
          <Field label="Name"><input name="name" value={form.name} onChange={onChange} required maxLength={200} /></Field>
          <Field label="Type">
            <select name="orgType" value={form.orgType} onChange={onChange}>
              {ORG_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
            </select>
          </Field>
          <Field label="Contract start"><input type="date" name="contractStartDate" value={form.contractStartDate} onChange={onChange} required /></Field>
          <Field label="Contract end"><input type="date" name="contractEndDate" value={form.contractEndDate} onChange={onChange} required /></Field>
          <button className="btn btn-primary" disabled={creating}>{creating ? 'Saving…' : 'Create school'}</button>
        </form>
        <ErrorNotice error={createError} />
      </section>

      <TenantAdminForm />

      <section className="card">
        <h2>All schools</h2>
        <ErrorNotice error={listError} />
        {tenants?.length === 0 && <EmptyState title="No schools yet" text="Create the first one above." />}
        {(isLoading || tenants?.length > 0) && (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Name</th><th>Code</th><th>Type</th><th>Status</th><th>Contract</th><th></th><th></th></tr></thead>
              <tbody>
                {isLoading && <SkeletonRows cols={7} />}
                {tenants?.map((t) => (
                  <tr key={t.id}>
                    <td><strong>{t.name}</strong></td>
                    <td>{t.code}</td>
                    <td>{t.orgType}</td>
                    <td><Chip tone={statusTone[t.status]}>{t.status}</Chip></td>
                    <td className="muted">{t.contractStartDate} → {t.contractEndDate}</td>
                    <td><button className="btn-link" onClick={() => navigator.clipboard.writeText(t.id)}>Copy ID</button></td>
                    <td>
                      <button className="btn btn-secondary" disabled={changing} onClick={() => toggle(t)}>
                        {t.status === 'ACTIVE' ? 'Suspend' : 'Activate'}
                      </button>
                    </td>
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