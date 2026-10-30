import { api } from './api';

export const noticesApi = api.injectEndpoints({
  endpoints: (build) => ({
    listNotices: build.query({ query: () => '/api/v1/notices', providesTags: ['Notice'] }),
    createNotice: build.mutation({
      query: (body) => ({ url: '/api/v1/notices', method: 'POST', body }),
      invalidatesTags: ['Notice'],
    }),
    archiveNotice: build.mutation({
      query: (id) => ({ url: `/api/v1/notices/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Notice'],
    }),
  }),
});

export const { useListNoticesQuery, useCreateNoticeMutation, useArchiveNoticeMutation } = noticesApi;