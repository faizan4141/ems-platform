import { createSlice, nanoid } from '@reduxjs/toolkit';

const uiSlice = createSlice({
  name: 'ui',
  initialState: { sidebarOpen: false, toasts: [] },
  reducers: {
    toggleSidebar: (state) => { state.sidebarOpen = !state.sidebarOpen; },
    closeSidebar: (state) => { state.sidebarOpen = false; },
    pushToast: {
      reducer: (state, action) => { state.toasts.push(action.payload); },
      prepare: (toast) => ({ payload: { id: nanoid(), tone: 'success', ...toast } }),
    },
    dismissToast: (state, action) => { state.toasts = state.toasts.filter((t) => t.id !== action.payload); },
  },
});

export const { toggleSidebar, closeSidebar, pushToast, dismissToast } = uiSlice.actions;
export default uiSlice.reducer;