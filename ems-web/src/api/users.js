import { api } from './client';

export const usersApi = {
  list: () => api('/api/v1/users'),
  create: (data) => api('/api/v1/users', { method: 'POST', body: data }),
};