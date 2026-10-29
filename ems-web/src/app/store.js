import { configureStore } from '@reduxjs/toolkit';
import authReducer from '../features/auth/authSlice';
import uiReducer from '../features/ui/uiSlice';
import { api } from '../services/api';

export const store = configureStore({
  reducer: { auth: authReducer, ui: uiReducer, [api.reducerPath]: api.reducer },
  middleware: (getDefault) =>
    getDefault({ serializableCheck: { ignoredPaths: [/^api\.mutations/] } }).concat(api.middleware),
});