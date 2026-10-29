import { api } from './api';

export const studentsApi = api.injectEndpoints({
  endpoints: (build) => ({
    listStudents: build.query({
      query: ({ page = 0, size = 20, search = '', sectionId = '', unassigned = false } = {}) => {
        const params = new URLSearchParams({ page, size });
        if (search) params.set('search', search);
        if (sectionId) params.set('sectionId', sectionId);
        if (unassigned) params.set('unassigned', 'true');
        return `/api/v1/students?${params}`;
      },
      providesTags: ['Student'],
    }),
    getStudent: build.query({ query: (id) => `/api/v1/students/${id}`, providesTags: ['Student'] }),
    createStudent: build.mutation({
      query: (body) => ({ url: '/api/v1/students', method: 'POST', body }),
      invalidatesTags: ['Student', 'Academic'],
    }),
    updateStudent: build.mutation({
      query: ({ id, ...body }) => ({ url: `/api/v1/students/${id}`, method: 'PUT', body }),
      invalidatesTags: ['Student'],
    }),
    importStudents: build.mutation({
      query: ({ file, dryRun }) => {
        const body = new FormData();
        body.append('file', file);
        return { url: `/api/v1/students/import?dryRun=${dryRun}`, method: 'POST', body };
      },
      invalidatesTags: (_result, _error, arg) => (arg.dryRun ? [] : ['Student', 'Academic']),
    }),
  }),
});

export const {
  useListStudentsQuery, useGetStudentQuery, useCreateStudentMutation,
  useUpdateStudentMutation, useImportStudentsMutation,
} = studentsApi;