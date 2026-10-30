import { api } from './api';

export const parentApi = api.injectEndpoints({
  endpoints: (build) => ({
    listChildren: build.query({ query: () => '/api/v1/parent/children', providesTags: ['Parent'] }),
    getChildToday: build.query({ query: (id) => `/api/v1/parent/children/${id}/today`, providesTags: ['Parent'] }),
  }),
});

export const { useListChildrenQuery, useGetChildTodayQuery } = parentApi;