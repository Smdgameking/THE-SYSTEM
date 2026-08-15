import { apiClient } from './client';

export async function getMemories(filter = {}) {
  const params = new URLSearchParams();
  if (filter.type) params.set('type', filter.type);
  if (filter.importance) params.set('importance', filter.importance);
  if (filter.source) params.set('source', filter.source);
  if (filter.search) params.set('search', filter.search);
  const query = params.toString();
  return apiClient(`/api/v1/memories${query ? `?${query}` : ''}`, { method: 'GET' });
}

export async function getMemory(memoryId) {
  return apiClient(`/api/v1/memories/${memoryId}`, { method: 'GET' });
}

export async function createMemory(payload) {
  return apiClient('/api/v1/memories', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateMemory(memoryId, payload) {
  return apiClient(`/api/v1/memories/${memoryId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export async function deleteMemory(memoryId) {
  return apiClient(`/api/v1/memories/${memoryId}`, { method: 'DELETE' });
}
