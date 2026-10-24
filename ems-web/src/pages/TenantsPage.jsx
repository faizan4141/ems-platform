import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { tenantsApi } from '../api/tenants';
import ErrorMessage from '../components/ErrorMessage';
import TenantAdminForm from '../components/TenantAdminForm';

const ORG_TYPES = ['SCHOOL', 'COLLEGE', 'UNIVERSITY', 'COACHING_INSTITUTE'];
const EMPTY_FORM = {
    code: '',
    name: '',
    orgType: 'SCHOOL',
    contractStartDate: '',
    contractEndDate: '',
};

export default function TenantsPage() {
    const queryClient = useQueryClient();
    const [form, setForm] = useState(EMPTY_FORM);

    const tenants = useQuery({ queryKey: ['tenants'], queryFn: tenantsApi.list });
    const refresh = () => queryClient.invalidateQueries({ queryKey: ['tenants'] });

    const createTenant = useMutation({
        mutationFn: tenantsApi.create,
        onSuccess: () => {
            setForm(EMPTY_FORM);
            refresh();
        },
    });

    const changeStatus = useMutation({
        mutationFn: ({ id, status }) => tenantsApi.updateStatus(id, status),
        onSuccess: refresh,
    });

    const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });
    const onSubmit = (e) => {
        e.preventDefault();
        createTenant.mutate(form);
    };

    const toggle = (tenant) => {
        const suspending = tenant.status === 'ACTIVE';
        if (suspending && !window.confirm(`Suspend ${tenant.name}? Its users will be blocked.`)) {
            return;
        }
        changeStatus.mutate({ id: tenant.id, status: suspending ? 'SUSPENDED' : 'ACTIVE' });
    };

    return (
        <>
            <h1>Schools</h1>

            <section className="card">
                <h2>Add a school</h2>
                <form onSubmit={onSubmit} className="grid">
                    <label>
                        Code
                        <input name="code" value={form.code} onChange={onChange} required
                            pattern="[a-z0-9\-]{3,50}"
                            title="3-50 characters: lowercase letters, digits, hyphens" />
                    </label>
                    <label>
                        Name
                        <input name="name" value={form.name} onChange={onChange} required maxLength={200} />
                    </label>
                    <label>
                        Type
                        <select name="orgType" value={form.orgType} onChange={onChange}>
                            {ORG_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                        </select>
                    </label>
                    <label>
                        Contract start
                        <input type="date" name="contractStartDate" value={form.contractStartDate}
                            onChange={onChange} required />
                    </label>
                    <label>
                        Contract end
                        <input type="date" name="contractEndDate" value={form.contractEndDate}
                            onChange={onChange} required />
                    </label>
                    <div className="actions">
                        <button type="submit" disabled={createTenant.isPending}>
                            {createTenant.isPending ? 'Saving…' : 'Create school'}
                        </button>
                    </div>
                </form>
                <ErrorMessage error={createTenant.error} />
            </section>

            <TenantAdminForm tenants={tenants.data ?? []} />

            <section className="card">
                <h2>All schools</h2>
                <ErrorMessage error={tenants.error || changeStatus.error} />
                {tenants.isPending && <p>Loading…</p>}
                {tenants.data?.length === 0 && <p>No schools yet.</p>}
                {tenants.data?.length > 0 && (
                    <div className="table-wrap">
                        <table>
                            <thead>
                                <tr>
                                    <th>Name</th><th>Code</th><th>Type</th><th>Status</th>
                                    <th>Contract</th><th>ID</th><th></th>
                                </tr>
                            </thead>
                            <tbody>
                                {tenants.data.map((t) => (
                                    <tr key={t.id}>
                                        <td>{t.name}</td>
                                        <td>{t.code}</td>
                                        <td>{t.orgType}</td>
                                        <td><span className={`badge ${t.status.toLowerCase()}`}>{t.status}</span></td>
                                        <td>{t.contractStartDate} → {t.contractEndDate}</td>
                                        <td>
                                            <button className="link" title={t.id}
                                                onClick={() => navigator.clipboard.writeText(t.id)}>
                                                Copy ID
                                            </button>
                                        </td>
                                        <td>
                                            <button className="secondary" onClick={() => toggle(t)}
                                                disabled={changeStatus.isPending}>
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