import { describe, expect, it } from 'vitest';
import reducer, { closeSidebar, dismissToast, pushToast, toggleSidebar } from './uiSlice';

describe('uiSlice', () => {
  it('adds a toast with an id and a default tone', () => {
    const state = reducer(undefined, pushToast({ text: 'Saved' }));
    expect(state.toasts).toHaveLength(1);
    expect(state.toasts[0]).toMatchObject({ text: 'Saved', tone: 'success' });
    expect(state.toasts[0].id).toBeTruthy();
  });
  it('dismisses a toast by id', () => {
    let state = reducer(undefined, pushToast({ text: 'A' }));
    state = reducer(state, dismissToast(state.toasts[0].id));
    expect(state.toasts).toHaveLength(0);
  });
  it('toggles and closes the sidebar', () => {
    let state = reducer(undefined, toggleSidebar());
    expect(state.sidebarOpen).toBe(true);
    state = reducer(state, closeSidebar());
    expect(state.sidebarOpen).toBe(false);
  });
});