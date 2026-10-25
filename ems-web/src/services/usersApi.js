import { api } from './api';

export const usersApi = api.injectEndpoints({
  endpoints: (build) => ({
    listUsers: build.query({ query: () => '/api/v1/users', providesTags: ['User'] }),
    createUser: build.mutation({
      query: (body) => ({ url: '/api/v1/users', method: 'POST', body }),
      invalidatesTags: ['User'],
    }),
    resendInvitation: build.mutation({
      query: (id) => ({ url: `/api/v1/users/${id}/invitation`, method: 'POST' }),
      invalidatesTags: ['User'],
    }),
    createTenantAdmin: build.mutation({
      query: (body) => ({ url: '/api/v1/users/tenant-admins', method: 'POST', body }),
      invalidatesTags: ['User'],
    }),
  }),
});

export const {
  useListUsersQuery, useCreateUserMutation, useResendInvitationMutation, useCreateTenantAdminMutation,
} = usersApi;