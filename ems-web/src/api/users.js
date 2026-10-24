import { api } from './client';

export const usersApi = {
  list: () => api('/api/v1/users'),
  create: (data) => api('/api/v1/users', { method: 'POST', body: data }),
  resendInvitation: (id) => api(`/api/v1/users/${id}/invitation`, { method: 'POST' }),
  createTenantAdmin: (data) => api('/api/v1/users/tenant-admins', { method: 'POST', body: data }),
};