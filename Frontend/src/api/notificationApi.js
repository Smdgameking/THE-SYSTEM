import { apiClient } from './client';

export async function getNotifications() {
  return apiClient('/api/v1/notifications', { method: 'GET' });
}

export async function getNotification(id) {
  return apiClient(`/api/v1/notifications/${id}`, { method: 'GET' });
}

export async function markNotificationRead(id) {
  return apiClient(`/api/v1/notifications/${id}/read`, { method: 'PATCH' });
}

export async function markAllNotificationsRead() {
  return apiClient('/api/v1/notifications/read-all', { method: 'PATCH' });
}

export async function deleteNotification(id) {
  return apiClient(`/api/v1/notifications/${id}`, { method: 'DELETE' });
}
