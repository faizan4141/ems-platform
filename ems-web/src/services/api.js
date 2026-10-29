import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import keycloak from '../keycloak';
import { config } from '../config';
import { friendlyMessage } from '../api/errors';

const rawBaseQuery = fetchBaseQuery({
  baseUrl: config.apiBaseUrl,
  prepareHeaders: async (headers) => {
    try {
      await keycloak.updateToken(30); // refresh if it expires within 30 seconds
    } catch {
      keycloak.login();
    }
    headers.set('Authorization', `Bearer ${keycloak.token}`);
    return headers;
  },
});

const baseQuery = async (args, api, extraOptions) => {
  const result = await rawBaseQuery(args, api, extraOptions);
  if (!result.error) return result;

  const status = typeof result.error.status === 'number' ? result.error.status : 0;
  const detail = result.error.data?.detail || result.error.data?.message;
  return {
    error: {
      status,
      message: friendlyMessage(status, detail),
      requestId: result.meta?.response?.headers.get('X-Request-Id') ?? null,
    },
  };
};

export const api = createApi({
  reducerPath: 'api',
  baseQuery,
  tagTypes: ['Tenant', 'User', 'Academic'],
  endpoints: () => ({}),
});