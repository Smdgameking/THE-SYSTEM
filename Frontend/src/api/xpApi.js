import { apiClient } from './client';

export async function getXpAccount() {
  return apiClient('/api/v1/xp/account', { method: 'GET' });
}

export async function getXpStatistics() {
  return apiClient('/api/v1/xp/statistics', { method: 'GET' });
}

export async function getXpStreak() {
  return apiClient('/api/v1/xp/streak', { method: 'GET' });
}

export async function getXpTransactions(page = 0, size = 15) {
  const params = new URLSearchParams();
  params.set('page', page);
  params.set('size', size);
  return apiClient(`/api/v1/xp/transactions?${params.toString()}`, { method: 'GET' });
}

export async function getAchievements() {
  return apiClient('/api/v1/xp/achievements', { method: 'GET' });
}

export async function getMyAchievements() {
  return apiClient('/api/v1/xp/achievements/user', { method: 'GET' });
}

export async function checkAchievements() {
  return apiClient('/api/v1/xp/achievements/check', { method: 'POST' });
}

export async function getLeaderboard(page = 0, size = 10) {
  const params = new URLSearchParams();
  params.set('page', page);
  params.set('size', size);
  return apiClient(`/api/v1/xp/leaderboard?${params.toString()}`, { method: 'GET' });
}
