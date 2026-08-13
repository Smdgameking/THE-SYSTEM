const API_BASE = 'http://localhost:9000';

let isRefreshing = false;
let refreshPromise = null;

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

  const response = await fetch(url, {
    ...options,
    headers,
  });

  const data = await response.json();

  if (response.status === 401 && !options._noAuthRetry) {
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
      const retryResponse = await fetch(url, {
        ...options,
        headers: retryHeaders,
      });
      const retryData = await retryResponse.json();
      return { status: retryResponse.status, data: retryData };
    }
  }

  return { status: response.status, data };
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

    const data = await result.json();

    if (result.ok && data.data) {
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
