import { apiClient } from './client';

export async function getDefinitionsByEngine(engine) {
  return apiClient(`/api/v1/settings/definitions/engine/${engine}`, { method: 'GET' });
}

export async function getNamespaceSettings(namespace) {
  return apiClient(`/api/v1/settings/${namespace}`, { method: 'GET' });
}

export async function setSetting(namespace, key, value) {
  return apiClient(`/api/v1/settings/${namespace}/${key}`, {
    method: 'PUT',
    body: JSON.stringify({ value }),
  });
}

export async function deleteSetting(namespace, key) {
  return apiClient(`/api/v1/settings/${namespace}/${key}`, { method: 'DELETE' });
}

export async function resetNamespace(namespace) {
  return apiClient(`/api/v1/settings/${namespace}/reset`, { method: 'POST' });
}
