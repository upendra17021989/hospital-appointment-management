import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';

const AuthContext = createContext(null);

const TOKEN_KEY   = 'hms_token';
const USER_KEY    = 'hms_user';

export const AuthProvider = ({ children }) => {
  const [token,   setToken]   = useState(() => localStorage.getItem(TOKEN_KEY));
  const [user,    setUser]    = useState(() => {
    try { return JSON.parse(localStorage.getItem(USER_KEY)); } catch { return null; }
  });
const [loading, setLoading] = useState(false);
  const [lastActivity, setLastActivity] = useState(Date.now());

  const IDLE_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes inactivity
  const CHECK_INTERVAL_MS = 30 * 1000; // 30 seconds check

  const isAuthenticated = !!token && !!user;

  const saveAuth = (tokenVal, userData) => {
    localStorage.setItem(TOKEN_KEY, tokenVal);
    localStorage.setItem(USER_KEY, JSON.stringify(userData));
    setToken(tokenVal);
    setUser(userData);
  };

const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setToken(null);
    setUser(null);
    window.location.href = '/login';
  }, []);


  // Verify token on mount
  useEffect(() => {
    if (!token) return;
    setLoading(true);
    const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
    fetch(`${API_BASE}/auth/me`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(r => r.json())
      .then(data => {
        if (data.success) {
          setUser(data.data);
          localStorage.setItem(USER_KEY, JSON.stringify(data.data));
        } else {
          logout();
        }
      })
      .catch(() => logout())
      .finally(() => setLoading(false));
  }, []); // run once on mount

  // Reset activity on login
  useEffect(() => {
    if (token) {
      setLastActivity(Date.now());
    }
  }, [token]);

  // Idle timeout detection
  useEffect(() => {
    if (!isAuthenticated) return;

    const resetActivity = () => setLastActivity(Date.now());
    
    const handleActivity = () => resetActivity();
    document.addEventListener('mousemove', handleActivity);
    document.addEventListener('keydown', handleActivity);

    const interval = setInterval(() => {
      if (Date.now() - lastActivity > IDLE_TIMEOUT_MS) {
        logout();
      }
    }, CHECK_INTERVAL_MS);

    return () => {
      document.removeEventListener('mousemove', handleActivity);
      document.removeEventListener('keydown', handleActivity);
      clearInterval(interval);
    };
  }, [isAuthenticated, lastActivity, logout]);

  return (
    <AuthContext.Provider value={{ token, user, isAuthenticated, loading, saveAuth, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
};
