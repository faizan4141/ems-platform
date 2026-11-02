import { api } from './api';

export const staffApi = api.injectEndpoints({
  endpoints: (build) => ({
    listStaff: build.query({ query: () => '/api/v1/staff', providesTags: ['Staff'] }),
    updateStaff: build.mutation({
      query: ({ id, ...body }) => ({ url: `/api/v1/staff/${id}`, method: 'PUT', body }),
      invalidatesTags: ['Staff'],
    }),
    assignTeacher: build.mutation({
      query: (body) => ({ url: '/api/v1/assignments', method: 'POST', body }),
      invalidatesTags: ['Staff'],
    }),
    unassignTeacher: build.mutation({
      query: (id) => ({ url: `/api/v1/assignments/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Staff'],
    }),
    getMyTeaching: build.query({ query: () => '/api/v1/me/teaching', providesTags: ['Staff'] }),
  }),
});

export const {
  useListStaffQuery, useUpdateStaffMutation, useAssignTeacherMutation, useUnassignTeacherMutation, useGetMyTeachingQuery,
} = staffApi;