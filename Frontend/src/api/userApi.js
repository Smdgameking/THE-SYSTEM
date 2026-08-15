import { apiClient } from './client';

export async function getCurrentUser() {
  return apiClient('/api/v1/users/me', {
    method: 'GET',
  });
}

export async function updateProfile(payload) {
  return apiClient('/api/v1/users/me', {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}
