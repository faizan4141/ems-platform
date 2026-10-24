export function resolveConfig(runtime = {}, env = {}) {
  return {
    apiBaseUrl: runtime.API_BASE_URL || env.VITE_API_BASE_URL,
    keycloakUrl: runtime.KEYCLOAK_URL || env.VITE_KEYCLOAK_URL,
    keycloakRealm: runtime.KEYCLOAK_REALM || env.VITE_KEYCLOAK_REALM,
    keycloakClientId: runtime.KEYCLOAK_CLIENT_ID || env.VITE_KEYCLOAK_CLIENT_ID,
  };
}

const runtime = (typeof window !== 'undefined' && window.__EMS_CONFIG__) || {};
export const config = resolveConfig(runtime, import.meta.env);