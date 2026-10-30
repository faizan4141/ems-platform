import { api } from './api';

const rosterUrl = (sectionId, date) =>
  `/api/v1/attendance/sections/${sectionId}${date ? `?date=${date}` : ''}`;

export const attendanceApi = api.injectEndpoints({
  endpoints: (build) => ({
    getRoster: build.query({
      query: ({ sectionId, date }) => rosterUrl(sectionId, date),
      providesTags: ['Attendance'],
    }),
    saveAttendance: build.mutation({
      query: ({ sectionId, date, marks }) => ({ url: rosterUrl(sectionId, date), method: 'PUT', body: { marks } }),
      invalidatesTags: ['Attendance'],
    }),
    getAttendanceSummary: build.query({ query: () => '/api/v1/attendance/summary', providesTags: ['Attendance'] }),
  }),
});

export const { useGetRosterQuery, useSaveAttendanceMutation, useGetAttendanceSummaryQuery } = attendanceApi;