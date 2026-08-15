const API_BASE = 'http://localhost:9000';

let isRefreshing = false;
let refreshPromise = null;

async function parseResponseBody(response) {
  const text = await response.text();
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
}

async function executeRequest(url, options, headers) {
  const response = await fetch(url, {
    ...options,
    headers,
  });
  const data = await parseResponseBody(response);
  return { status: response.status, data };
}

export async function apiClient(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  const storedTokens = getStoredTokens();
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  if (storedTokens.accessToken && !headers.Authorization) {
    headers.Authorization = `Bearer ${storedTokens.accessToken}`;
  }

  let result = await executeRequest(url, options, headers);

  if (result.status === 401 && !options._noAuthRetry) {
    if (isRefreshing) {
      await refreshPromise;
    } else {
      isRefreshing = true;
      refreshPromise = attemptRefresh();
      await refreshPromise;
      isRefreshing = false;
      refreshPromise = null;
    }

    const accessToken = localStorage.getItem('accessToken');
    if (accessToken) {
      const retryHeaders = {
        ...headers,
        Authorization: `Bearer ${accessToken}`,
      };
      result = await executeRequest(url, options, retryHeaders);
    }
  }

  return result;
}

async function attemptRefresh() {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) {
    clearAuth();
    return;
  }

  try {
    const result = await fetch(`${API_BASE}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });

    const data = await parseResponseBody(result);

    if (result.ok && data && data.data) {
      localStorage.setItem('accessToken', data.data.accessToken);
      localStorage.setItem('refreshToken', data.data.refreshToken);
    } else {
      clearAuth();
    }
  } catch {
    clearAuth();
  }
}

export function clearAuth() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
}

export function getStoredTokens() {
  return {
    accessToken: localStorage.getItem('accessToken'),
    refreshToken: localStorage.getItem('refreshToken'),
  };
}
