import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { api, type User } from './api';

interface AuthState {
  user: User | null;
  loading: boolean;
  login: (username: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  /** Nach einem 401 aufrufen, z. B. wenn die Sitzung abgelaufen ist. */
  sessionExpired: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  // Beim Laden pruefen, ob noch eine Sitzung besteht. Nebenbei setzt das
  // Backend dabei das CSRF-Cookie fuer die folgenden POST-Aufrufe.
  useEffect(() => {
    api
      .me()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  const login = useCallback(async (username: string, password: string) => {
    setUser(await api.login(username, password));
  }, []);

  const register = useCallback(async (username: string, email: string, password: string) => {
    await api.register(username, email, password);
    setUser(await api.login(username, password));
  }, []);

  const logout = useCallback(async () => {
    await api.logout().catch(() => undefined);
    setUser(null);
  }, []);

  const sessionExpired = useCallback(() => setUser(null), []);

  const value = useMemo(
    () => ({ user, loading, login, register, logout, sessionExpired }),
    [user, loading, login, register, logout, sessionExpired],
  );
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth muss innerhalb von AuthProvider verwendet werden');
  }
  return ctx;
}

/** Geschuetzte Seite: ohne Anmeldung geht es zum Login und danach zurueck. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <p className="centered muted">Lade …</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <>{children}</>;
}
