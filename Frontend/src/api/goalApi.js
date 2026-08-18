import { apiClient } from './client';

export async function getGoals(filter = {}) {
  const params = new URLSearchParams();
  if (filter.status) params.set('status', filter.status);
  if (filter.priority) params.set('priority', filter.priority);
  if (filter.category) params.set('category', filter.category);
  if (filter.page) params.set('page', filter.page);
  if (filter.size) params.set('size', filter.size);
  const query = params.toString();
  return apiClient(`/api/v1/goals${query ? `?${query}` : ''}`, { method: 'GET' });
}

export async function getGoal(goalId) {
  return apiClient(`/api/v1/goals/${goalId}`, { method: 'GET' });
}

export async function createGoal(payload) {
  return apiClient('/api/v1/goals', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateGoal(goalId, payload) {
  return apiClient(`/api/v1/goals/${goalId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function deleteGoal(goalId) {
  return apiClient(`/api/v1/goals/${goalId}`, { method: 'DELETE' });
}

export async function startGoal(goalId) {
  return apiClient(`/api/v1/goals/${goalId}/start`, { method: 'POST' });
}

export async function pauseGoal(goalId) {
  return apiClient(`/api/v1/goals/${goalId}/pause`, { method: 'POST' });
}

export async function resumeGoal(goalId) {
  return apiClient(`/api/v1/goals/${goalId}/resume`, { method: 'POST' });
}

export async function completeGoal(goalId) {
  return apiClient(`/api/v1/goals/${goalId}/complete`, { method: 'POST' });
}

export async function failGoal(goalId, reason) {
  return apiClient(`/api/v1/goals/${goalId}/fail`, {
    method: 'POST',
    body: JSON.stringify(reason ?? ''),
  });
}

export async function archiveGoal(goalId) {
  return apiClient(`/api/v1/goals/${goalId}/archive`, { method: 'POST' });
}

export async function updateGoalProgress(goalId, progress) {
  const params = new URLSearchParams();
  params.set('progress', progress);
  return apiClient(`/api/v1/goals/${goalId}/progress?${params.toString()}`, {
    method: 'PUT',
  });
}

export async function getGoalStatistics() {
  return apiClient('/api/v1/goals/statistics', { method: 'GET' });
}

export async function getGoalMilestones(goalId) {
  return apiClient(`/api/v1/goals/${goalId}/milestones`, { method: 'GET' });
}

export async function createGoalMilestone(goalId, payload) {
  return apiClient(`/api/v1/goals/${goalId}/milestones`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateGoalMilestone(goalId, milestoneId, payload) {
  return apiClient(`/api/v1/goals/${goalId}/milestones/${milestoneId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function completeGoalMilestone(goalId, milestoneId) {
  return apiClient(`/api/v1/goals/${goalId}/milestones/${milestoneId}/complete`, {
    method: 'POST',
  });
}

export async function deleteGoalMilestone(goalId, milestoneId) {
  return apiClient(`/api/v1/goals/${goalId}/milestones/${milestoneId}`, {
    method: 'DELETE',
  });
}
