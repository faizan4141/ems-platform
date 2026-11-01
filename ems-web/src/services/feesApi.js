import { api } from './api';

const post = (url, body) => ({ url, method: 'POST', body });

export const feesApi = api.injectEndpoints({
  endpoints: (build) => ({
    getFeeSummary: build.query({ query: () => '/api/v1/fees/summary', providesTags: ['Fees'] }),
    getDues: build.query({
      query: (classId) => `/api/v1/fees/dues${classId ? `?classId=${classId}` : ''}`,
      providesTags: ['Fees'],
    }),
    listFeeItems: build.query({ query: (classId) => `/api/v1/fees/items?classId=${classId}`, providesTags: ['Fees'] }),
    createFeeItem: build.mutation({ query: (body) => post('/api/v1/fees/items', body), invalidatesTags: ['Fees'] }),
    deleteFeeItem: build.mutation({
      query: (id) => ({ url: `/api/v1/fees/items/${id}`, method: 'DELETE' }), invalidatesTags: ['Fees'],
    }),
    generateCharges: build.mutation({
      query: (classId) => post(`/api/v1/fees/classes/${classId}/generate`), invalidatesTags: ['Fees'],
    }),
    getStudentLedger: build.query({ query: (id) => `/api/v1/fees/students/${id}`, providesTags: ['Fees'] }),
    recordPayment: build.mutation({
      query: ({ chargeId, ...body }) => post(`/api/v1/fees/charges/${chargeId}/payments`, body),
      invalidatesTags: ['Fees'],
    }),
    voidPayment: build.mutation({
      query: ({ paymentId, reason }) => post(`/api/v1/fees/payments/${paymentId}/void`, { reason }),
      invalidatesTags: ['Fees'],
    }),
    runReminders: build.mutation({ query: () => post('/api/v1/fees/reminders/run'), invalidatesTags: ['Fees'] }),
    getChildFees: build.query({ query: (id) => `/api/v1/parent/children/${id}/fees`, providesTags: ['Fees'] }),
    startPayment: build.mutation({
      query: ({ studentId, ...body }) => post(`/api/v1/parent/children/${studentId}/pay`, body),
    }),
    getAttempt: build.query({ query: (id) => `/api/v1/parent/payment-attempts/${id}` }),
    completeSimulated: build.mutation({
      query: ({ providerRef, outcome }) => post(`/api/v1/payments/simulated/${providerRef}/complete?outcome=${outcome}`),
      invalidatesTags: ['Fees'],
    }),
    listOnlinePayments: build.query({ query: () => '/api/v1/fees/online-payments', providesTags: ['Fees'] }),
  }),
});

export const {
  useGetFeeSummaryQuery, useGetDuesQuery, useListFeeItemsQuery, useCreateFeeItemMutation, useDeleteFeeItemMutation,
  useGenerateChargesMutation, useGetStudentLedgerQuery, useRecordPaymentMutation, useVoidPaymentMutation,
  useRunRemindersMutation, useGetChildFeesQuery, useStartPaymentMutation, useGetAttemptQuery, useCompleteSimulatedMutation, useListOnlinePaymentsQuery,
} = feesApi;