import { createSlice } from '@reduxjs/toolkit';

export function profileFromToken(claims = {}) {
  return {
    name: claims.name || claims.preferred_username || '',
    firstName: claims.given_name || (claims.name || '').split(' ')[0] || '',
    email: claims.email || '',
    roles: claims.realm_access?.roles ?? [],
    tenantId: claims.tenant_id ?? null,
  };
}

const initialState = { status: 'loading', name: '', firstName: '', email: '', roles: [], tenantId: null };

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    authReady: (_state, action) => ({ ...action.payload, status: 'ready' }),
    authFailed: (state) => { state.status = 'error'; },
  },
});

export const { authReady, authFailed } = authSlice.actions;
export default authSlice.reducer;