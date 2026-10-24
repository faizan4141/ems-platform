import { describe, expect, it } from 'vitest';
import { resolveConfig } from './config';

const env = {
  VITE_API_BASE_URL: 'http://dev-api',
  VITE_KEYCLOAK_URL: 'http://dev-kc',
  VITE_KEYCLOAK_REALM: 'ems',
  VITE_KEYCLOAK_CLIENT_ID: 'ems-web',
};

describe('resolveConfig', () => {
  it('falls back to build-time env when there is no runtime config', () => {
    expect(resolveConfig({}, env).apiBaseUrl).toBe('http://dev-api');
  });

  it('prefers runtime config over build-time env', () => {
    const cfg = resolveConfig({ API_BASE_URL: 'https://api.example.com' }, env);
    expect(cfg.apiBaseUrl).toBe('https://api.example.com');
    expect(cfg.keycloakUrl).toBe('http://dev-kc');
  });

  it('ignores empty runtime values', () => {
    expect(resolveConfig({ API_BASE_URL: '' }, env).apiBaseUrl).toBe('http://dev-api');
  });
});