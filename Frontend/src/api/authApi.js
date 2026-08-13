import { apiClient } from './client';

export async function register(username, email, password) {
  return apiClient('/api/v1/auth/register', {
    method: 'POST',
    body: JSON.stringify({ username, email, password }),
  });
}

export async function login(username, password) {
  return apiClient('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });
}

export async function refreshToken(refreshToken) {
  return apiClient('/api/v1/auth/refresh', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  });
}

export async function logout(refreshToken) {
  return apiClient('/api/v1/auth/logout', {
    method: 'POST',
    body: JSON.stringify({ refreshToken }),
  });
}
