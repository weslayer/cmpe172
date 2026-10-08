// Thin fetch wrapper. The browser sends the session cookie itself (same origin through the Vite proxy);
// state-changing requests echo the XSRF-TOKEN cookie back as a header, which is what Spring Security checks.

export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function xsrfToken() {
  const cookie = document.cookie.split('; ').find((c) => c.startsWith('XSRF-TOKEN='));
  return cookie ? decodeURIComponent(cookie.split('=')[1]) : '';
}

export async function api(path, { method = 'GET', json, form } = {}) {
  const headers = {};
  let body;
  if (json !== undefined) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(json);
  } else if (form) {
    body = new URLSearchParams(form);
  }
  if (method !== 'GET') {
    headers['X-XSRF-TOKEN'] = xsrfToken();
  }

  const res = await fetch('/api' + path, { method, headers, body });
  if (res.status === 204) {
    return null;
  }
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    throw new ApiError(res.status, data?.message ?? res.statusText);
  }
  return data;
}

export function formatMoney(amount) {
  return `$${Number(amount).toFixed(2)}`;
}

export function formatTime(iso) {
  return new Date(iso).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
}
