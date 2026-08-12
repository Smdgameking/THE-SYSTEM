import { useState, useCallback } from 'react';
import { AuthContext } from './AuthContextDef';
import { clearAuth, getStoredTokens } from '../api/client';
import * as authApi from '../api/authApi';
import * as userApi from '../api/userApi';

export function AuthProvider({ children }) {
  const [accessToken, setAccessToken] = useState(() => localStorage.getItem('accessToken'));
  const [refreshToken, setRefreshToken] = useState(() => localStorage.getItem('refreshToken'));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(() => {
    const tokens = getStoredTokens();
    return !!(tokens.accessToken && tokens.refreshToken);
  });
  const [error, setError] = useState(null);

  const syncTokens = useCallback(() => {
    const tokens = getStoredTokens();
    setAccessToken(tokens.accessToken);
    setRefreshToken(tokens.refreshToken);
  }, []);

  const loadCurrentUser = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await userApi.getCurrentUser();
      if (result.status === 200 && result.data?.data) {
        setUser(result.data.data);
      } else {
        setUser(null);
      }
    } catch (err) {
      setError(err.message);
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  const initialize = useCallback(async () => {
    const tokens = getStoredTokens();
    if (tokens.accessToken && tokens.refreshToken) {
      await loadCurrentUser();
    } else {
      setLoading(false);
    }
  }, [loadCurrentUser]);

  useState(() => {
    initialize();
  });

  const register = useCallback(async (email, password) => {
    setError(null);
    const result = await authApi.register(email, password);
    if (result.status === 201 && result.data?.data) {
      const { accessToken: at, refreshToken: rt } = result.data.data;
      localStorage.setItem('accessToken', at);
      localStorage.setItem('refreshToken', rt);
      syncTokens();
      await loadCurrentUser();
      return { success: true, data: result.data };
    }
    return { success: false, status: result.status, data: result.data };
  }, [syncTokens, loadCurrentUser]);

  const login = useCallback(async (email, password) => {
    setError(null);
    const result = await authApi.login(email, password);
    if (result.status === 200 && result.data?.data) {
      const { accessToken: at, refreshToken: rt } = result.data.data;
      localStorage.setItem('accessToken', at);
      localStorage.setItem('refreshToken', rt);
      syncTokens();
      await loadCurrentUser();
      return { success: true, data: result.data };
    }
    return { success: false, status: result.status, data: result.data };
  }, [syncTokens, loadCurrentUser]);

  const refresh = useCallback(async () => {
    setError(null);
    const rt = localStorage.getItem('refreshToken');
    if (!rt) {
      return { success: false, status: 400, data: { message: 'No refresh token' } };
    }
    const result = await authApi.refreshToken(rt);
    if (result.status === 200 && result.data?.data) {
      const { accessToken: at, refreshToken: newRt } = result.data.data;
      localStorage.setItem('accessToken', at);
      localStorage.setItem('refreshToken', newRt);
      syncTokens();
      return { success: true, data: result.data };
    }
    return { success: false, status: result.status, data: result.data };
  }, [syncTokens]);

  const logout = useCallback(async () => {
    setError(null);
    const rt = localStorage.getItem('refreshToken');
    if (rt) {
      await authApi.logout(rt);
    }
    clearAuth();
    syncTokens();
    setUser(null);
  }, [syncTokens]);

  const value = {
    accessToken,
    refreshToken,
    user,
    loading,
    error,
    authenticated: !!accessToken && !!user,
    register,
    login,
    refresh,
    logout,
    loadCurrentUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
