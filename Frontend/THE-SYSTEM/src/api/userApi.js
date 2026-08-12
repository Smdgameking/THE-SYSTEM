import { apiClient } from './client';

export async function getCurrentUser() {
  return apiClient('/api/v1/users/me', {
    method: 'GET',
  });
}
