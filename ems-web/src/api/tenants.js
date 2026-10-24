import { api } from './client';

export const tenantsApi = {
  list: () => api('/api/v1/tenants'),
  create: (data) => api('/api/v1/tenants', { method: 'POST', body: data }),
  updateStatus: (id, status) =>
    api(`/api/v1/tenants/${id}/status`, { method: 'PATCH', body: { status } }),
};