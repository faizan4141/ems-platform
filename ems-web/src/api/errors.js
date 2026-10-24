export function friendlyMessage(status, detail) {
  switch (status) {
    case 401:
      return 'Your session has expired. Please sign in again.';
    case 403:
      return detail || "You don't have permission to do that.";
    case 404:
      return detail || 'That item could not be found.';
    case 409:
      return detail || 'That already exists.';
    case 429:
      return 'Too many requests. Please wait a few seconds and try again.';
    case 503:
      return 'The service is temporarily unavailable. Please try again shortly.';
    case 502:
      return detail || 'A connected service is not responding. Please try again shortly.';
    default:
      return status >= 500
        ? 'Something went wrong on our side. Please try again.'
        : detail || 'The request could not be completed.';
  }
}