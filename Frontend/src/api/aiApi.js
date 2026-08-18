import { apiClient } from './client';

export async function createAiInteraction(payload) {
  return apiClient('/api/v1/ai/interactions', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function getAiInteractions() {
  return apiClient('/api/v1/ai/interactions', { method: 'GET' });
}

export async function getAiInteraction(interactionId) {
  return apiClient(`/api/v1/ai/interactions/${interactionId}`, { method: 'GET' });
}

export async function deleteAiInteraction(interactionId) {
  return apiClient(`/api/v1/ai/interactions/${interactionId}`, { method: 'DELETE' });
}
