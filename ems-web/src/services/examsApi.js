import { api } from './api';

const send = (url, method, body) => ({ url, method, body });

export const examsApi = api.injectEndpoints({
  endpoints: (build) => ({
    listSubjects: build.query({ query: () => '/api/v1/subjects', providesTags: ['Exams'] }),
    createSubject: build.mutation({ query: (body) => send('/api/v1/subjects', 'POST', body), invalidatesTags: ['Exams'] }),
    deleteSubject: build.mutation({ query: (id) => send(`/api/v1/subjects/${id}`, 'DELETE'), invalidatesTags: ['Exams'] }),
    getScale: build.query({ query: () => '/api/v1/grading-scale', providesTags: ['Exams'] }),
    saveScale: build.mutation({ query: (bands) => send('/api/v1/grading-scale', 'PUT', { bands }), invalidatesTags: ['Exams'] }),
    listExams: build.query({ query: (classId) => `/api/v1/exams?classId=${classId}`, providesTags: ['Exams'] }),
    createExam: build.mutation({ query: (body) => send('/api/v1/exams', 'POST', body), invalidatesTags: ['Exams'] }),
    getExam: build.query({ query: (id) => `/api/v1/exams/${id}`, providesTags: ['Exams'] }),
    getGrid: build.query({
      query: ({ examId, sectionId }) => `/api/v1/exams/${examId}/marks${sectionId ? `?sectionId=${sectionId}` : ''}`,
      providesTags: ['Exams'],
    }),
    saveMarks: build.mutation({
      query: ({ examId, entries }) => send(`/api/v1/exams/${examId}/marks`, 'PUT', { entries }),
      invalidatesTags: ['Exams'],
    }),
    getResults: build.query({
      query: ({ examId, sectionId }) => `/api/v1/exams/${examId}/results${sectionId ? `?sectionId=${sectionId}` : ''}`,
      providesTags: ['Exams'],
    }),
    publishExam: build.mutation({ query: (id) => send(`/api/v1/exams/${id}/publish`, 'POST'), invalidatesTags: ['Exams'] }),
    reopenExam: build.mutation({
      query: ({ examId, reason }) => send(`/api/v1/exams/${examId}/reopen`, 'POST', { reason }),
      invalidatesTags: ['Exams'],
    }),
    getAudit: build.query({ query: (id) => `/api/v1/exams/${id}/audit`, providesTags: ['Exams'] }),
    getReportCard: build.query({
      query: ({ examId, studentId }) => `/api/v1/exams/${examId}/report-card/${studentId}`, providesTags: ['Exams'],
    }),
    listChildResults: build.query({ query: (studentId) => `/api/v1/parent/children/${studentId}/results`, providesTags: ['Exams'] }),
    getChildReportCard: build.query({
      query: ({ studentId, examId }) => `/api/v1/parent/children/${studentId}/results/${examId}`, providesTags: ['Exams'],
    }),
  }),
});

export const {
  useListSubjectsQuery, useCreateSubjectMutation, useDeleteSubjectMutation, useGetScaleQuery, useSaveScaleMutation,
  useListExamsQuery, useCreateExamMutation, useGetExamQuery, useGetGridQuery, useSaveMarksMutation, useGetResultsQuery,
  usePublishExamMutation, useReopenExamMutation, useGetAuditQuery, useGetReportCardQuery,
  useListChildResultsQuery, useGetChildReportCardQuery,
} = examsApi;