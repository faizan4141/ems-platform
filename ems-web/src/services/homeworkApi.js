import { api } from './api';

export const homeworkApi = api.injectEndpoints({
  endpoints: (build) => ({
    listHomework: build.query({
      query: ({ sectionId = '', kind = '' } = {}) => {
        const params = new URLSearchParams();
        if (sectionId) params.set('sectionId', sectionId);
        if (kind) params.set('kind', kind);
        const text = params.toString();
        return `/api/v1/homework${text ? `?${text}` : ''}`;
      },
      providesTags: ['Homework'],
    }),
    createHomework: build.mutation({
      query: (body) => ({ url: '/api/v1/homework', method: 'POST', body }), invalidatesTags: ['Homework'],
    }),
    updateHomework: build.mutation({
      query: ({ id, ...body }) => ({ url: `/api/v1/homework/${id}`, method: 'PUT', body }), invalidatesTags: ['Homework'],
    }),
    deleteHomework: build.mutation({
      query: (id) => ({ url: `/api/v1/homework/${id}`, method: 'DELETE' }), invalidatesTags: ['Homework'],
    }),
    getChildHomework: build.query({
      query: (studentId) => `/api/v1/parent/children/${studentId}/homework`, providesTags: ['Homework'],
    }),
  }),
});

export const {
  useListHomeworkQuery, useCreateHomeworkMutation, useUpdateHomeworkMutation, useDeleteHomeworkMutation,
  useGetChildHomeworkQuery,
} = homeworkApi;