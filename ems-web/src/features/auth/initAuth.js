import keycloak from '../../keycloak';
import { authReady, authFailed, profileFromToken } from './authSlice';

// StrictMode runs effects twice in development; Keycloak must initialize exactly once.
let initPromise = null;

export const initAuth = () => async (dispatch) => {
  if (!initPromise) {
    initPromise = keycloak.init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false });
  }
  try {
    await initPromise;
    dispatch(authReady(profileFromToken(keycloak.tokenParsed)));
  } catch {
    dispatch(authFailed());
  }
};

export const logout = () => keycloak.logout({ redirectUri: window.location.origin });