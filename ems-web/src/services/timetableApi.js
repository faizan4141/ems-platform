import { api } from './api';

const slotUrl = ({ sectionId, day, periodId }) => `/api/v1/timetable/sections/${sectionId}/slots/${day}/${periodId}`;

export const timetableApi = api.injectEndpoints({
  endpoints: (build) => ({
    listPeriods: build.query({ query: () => '/api/v1/periods', providesTags: ['Timetable'] }),
    createPeriod: build.mutation({
      query: (body) => ({ url: '/api/v1/periods', method: 'POST', body }), invalidatesTags: ['Timetable'],
    }),
    updatePeriod: build.mutation({
      query: ({ id, ...body }) => ({ url: `/api/v1/periods/${id}`, method: 'PUT', body }), invalidatesTags: ['Timetable'],
    }),
    deletePeriod: build.mutation({
      query: (id) => ({ url: `/api/v1/periods/${id}`, method: 'DELETE' }), invalidatesTags: ['Timetable'],
    }),
    getSectionTimetable: build.query({
      query: (sectionId) => `/api/v1/timetable/sections/${sectionId}`, providesTags: ['Timetable'],
    }),
    setSlot: build.mutation({
      query: ({ sectionId, day, periodId, ...body }) => ({ url: slotUrl({ sectionId, day, periodId }), method: 'PUT', body }),
      invalidatesTags: ['Timetable'],
    }),
    clearSlot: build.mutation({
      query: (slot) => ({ url: slotUrl(slot), method: 'DELETE' }), invalidatesTags: ['Timetable'],
    }),
    getMyTimetable: build.query({ query: () => '/api/v1/me/timetable', providesTags: ['Timetable'] }),
    getMyToday: build.query({ query: () => '/api/v1/me/today', providesTags: ['Timetable'] }),
    getChildTimetable: build.query({
      query: (studentId) => `/api/v1/parent/children/${studentId}/timetable`, providesTags: ['Timetable'],
    }),
  }),
});

export const {
  useListPeriodsQuery, useCreatePeriodMutation, useUpdatePeriodMutation, useDeletePeriodMutation,
  useGetSectionTimetableQuery, useSetSlotMutation, useClearSlotMutation, useGetMyTimetableQuery,
  useGetMyTodayQuery, useGetChildTimetableQuery,
} = timetableApi;