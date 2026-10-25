import { api } from './api';

export const tenantsApi = api.injectEndpoints({
  endpoints: (build) => ({
    listTenants: build.query({ query: () => '/api/v1/tenants', providesTags: ['Tenant'] }),
    getTenant: build.query({ query: (id) => `/api/v1/tenants/${id}`, providesTags: ['Tenant'] }),
    createTenant: build.mutation({
      query: (body) => ({ url: '/api/v1/tenants', method: 'POST', body }),
      invalidatesTags: ['Tenant'],
    }),
    updateTenantStatus: build.mutation({
      query: ({ id, status }) => ({ url: `/api/v1/tenants/${id}/status`, method: 'PATCH', body: { status } }),
      invalidatesTags: ['Tenant'],
    }),
  }),
});

export const {
  useListTenantsQuery, useGetTenantQuery, useCreateTenantMutation, useUpdateTenantStatusMutation,
} = tenantsApi;