import { api } from './api';

const write = (url, method, body) => ({ url, method, body });

export const academicApi = api.injectEndpoints({
  endpoints: (build) => ({
    getProfile: build.query({ query: () => '/api/v1/school/profile', providesTags: ['Academic'] }),
    saveProfile: build.mutation({
      query: (body) => write('/api/v1/school/profile', 'PUT', body), invalidatesTags: ['Academic'],
    }),
    listYears: build.query({ query: () => '/api/v1/academic-years', providesTags: ['Academic'] }),
    createYear: build.mutation({
      query: (body) => write('/api/v1/academic-years', 'POST', body), invalidatesTags: ['Academic'],
    }),
    setCurrentYear: build.mutation({
      query: (id) => write(`/api/v1/academic-years/${id}/current`, 'PUT'), invalidatesTags: ['Academic'],
    }),
    listClasses: build.query({
      query: (yearId) => `/api/v1/classes${yearId ? `?academicYearId=${yearId}` : ''}`,
      providesTags: ['Academic'],
    }),
    createClass: build.mutation({
      query: (body) => write('/api/v1/classes', 'POST', body), invalidatesTags: ['Academic'],
    }),
    getOnboarding: build.query({ query: () => '/api/v1/onboarding', providesTags: ['Academic'] }),
  }),
});

export const {
  useGetProfileQuery, useSaveProfileMutation, useListYearsQuery, useCreateYearMutation,
  useSetCurrentYearMutation, useListClassesQuery, useCreateClassMutation, useGetOnboardingQuery,
} = academicApi;