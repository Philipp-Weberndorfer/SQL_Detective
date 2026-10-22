import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth, useAuth } from './auth';
import EditorPage from './pages/EditorPage';
import HomePage from './pages/HomePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';

export default function App() {
  const { user, logout } = useAuth();

  return (
    <div className="app">
      <header className="topbar">
        <Link to="/" className="brand">
          SQL Detective
        </Link>
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
            path="/editor"
            element={
              <RequireAuth>
                <EditorPage />
              </RequireAuth>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}
