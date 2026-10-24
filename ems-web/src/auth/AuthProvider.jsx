/* eslint-disable react-refresh/only-export-components */

import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import keycloak from '../keycloak';

const AuthContext = createContext(null);

// React StrictMode runs effects twice in development, but Keycloak must be
// initialized exactly once, so the promise is kept outside the component.
let initPromise = null;
function initKeycloak() {
  if (!initPromise) {
    initPromise = keycloak.init({
      onLoad: 'login-required', // redirect to the Keycloak login page if not signed in
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });
  }
  return initPromise;
}

export function AuthProvider({ children }) {
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    initKeycloak()
      .then(() => setStatus('ready'))
      .catch(() => setStatus('error'));
  }, []);

  const value = useMemo(() => {
    const claims = keycloak.tokenParsed ?? {};
    return {
      status,
      name: claims.name || claims.preferred_username || '',
      email: claims.email || '',
      roles: claims.realm_access?.roles ?? [],
      tenantId: claims.tenant_id ?? null,
      logout: () => keycloak.logout({ redirectUri: window.location.origin }),
    };
  }, [status]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}