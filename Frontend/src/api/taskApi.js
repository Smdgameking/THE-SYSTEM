import { apiClient } from './client';

export async function getTasks(filter = {}) {
  const params = new URLSearchParams();
  if (filter.status) params.set('status', filter.status);
  if (filter.priority) params.set('priority', filter.priority);
  if (filter.goalId) params.set('goalId', filter.goalId);
  if (filter.search) params.set('search', filter.search);
  if (filter.page) params.set('page', filter.page);
  if (filter.limit) params.set('limit', filter.limit);
  const query = params.toString();
  return apiClient(`/api/v1/tasks${query ? `?${query}` : ''}`, { method: 'GET' });
}

export async function getTask(taskId) {
  return apiClient(`/api/v1/tasks/${taskId}`, { method: 'GET' });
}

export async function createTask(payload) {
  return apiClient('/api/v1/tasks', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateTask(taskId, payload) {
  return apiClient(`/api/v1/tasks/${taskId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export async function deleteTask(taskId) {
  return apiClient(`/api/v1/tasks/${taskId}`, { method: 'DELETE' });
}

export async function completeTask(taskId) {
  return apiClient(`/api/v1/tasks/${taskId}/complete`, { method: 'POST' });
}

export async function failTask(taskId, reason) {
  return apiClient(`/api/v1/tasks/${taskId}/fail`, {
    method: 'POST',
    body: JSON.stringify(reason ?? ''),
  });
}

export async function cancelTask(taskId, reason) {
  return apiClient(`/api/v1/tasks/${taskId}/cancel`, {
    method: 'POST',
    body: JSON.stringify(reason ?? ''),
  });
}

export async function archiveTask(taskId) {
  return apiClient(`/api/v1/tasks/${taskId}/archive`, { method: 'POST' });
}

export async function restoreTask(taskId) {
  return apiClient(`/api/v1/tasks/${taskId}/restore`, { method: 'POST' });
}

export async function getTaskStatistics() {
  return apiClient('/api/v1/tasks/statistics', { method: 'GET' });
}
