import { Link, Navigate, NavLink, Route, Routes } from 'react-router-dom';
import { RequireAuth, useAuth } from './auth';
import CasePage from './pages/CasePage';
import CasesPage from './pages/CasesPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';

export default function App() {
  const { user, logout } = useAuth();

  return (
    <div className="app">
      <header className="topbar">
        <div className="topbar-nav">
          <Link to="/" className="brand">
            SQL Detective
          </Link>
          {user && <NavLink to="/faelle">Fälle</NavLink>}
        </div>
        {user && (
          <div className="topbar-user">
            <span className="muted">Angemeldet als {user.username}</span>
            <button type="button" className="secondary" onClick={logout}>
              Abmelden
            </button>
          </div>
        )}
      </header>

      <main className="content">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route
            path="/faelle"
            element={
              <RequireAuth>
                <CasesPage />
              </RequireAuth>
            }
          />
          <Route
            path="/faelle/:caseId"
            element={
              <RequireAuth>
                <CasePage />
              </RequireAuth>
            }
          />
          {/* alte Adresse aus Sprint 1 */}
          <Route path="/editor" element={<Navigate to="/faelle" replace />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
