import keycloak from '../keycloak';
import { friendlyMessage } from './errors';

const BASE_URL = import.meta.env.VITE_API_BASE_URL;

export class ApiError extends Error {
  constructor(status, message, requestId) {
    super(message);
    this.status = status;
    this.requestId = requestId;
  }
}

export async function api(path, { method = 'GET', body } = {}) {
  try {
    await keycloak.updateToken(30); // refresh if it expires within 30 seconds
  } catch {
    keycloak.login();
    throw new ApiError(401, friendlyMessage(401));
  }

  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers: {
        Authorization: `Bearer ${keycloak.token}`,
        ...(body ? { 'Content-Type': 'application/json' } : {}),
      },
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Cannot reach the server. Check your connection and try again.');
  }

  const requestId = response.headers.get('X-Request-Id');
  if (response.ok) {
    return response.status === 204 ? null : response.json();
  }

  let detail;
  try {
    const problem = await response.json();
    detail = problem.detail || problem.message;
  } catch {
    /* empty body, e.g. a bare 403 */
  }
  throw new ApiError(response.status, friendlyMessage(response.status, detail), requestId);
}