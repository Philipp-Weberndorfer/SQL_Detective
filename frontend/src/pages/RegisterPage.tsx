import { useState, type FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { ApiError } from '../api';
import { useAuth } from '../auth';

export default function RegisterPage() {
  const { user, register } = useAuth();
  const navigate = useNavigate();

  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (user) {
    return <Navigate to="/editor" replace />;
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!username.trim() || !email.trim() || !password) {
      setError('Bitte fülle alle Felder aus.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await register(username.trim(), email.trim(), password);
      navigate('/editor', { replace: true });
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Registrierung fehlgeschlagen.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="card form" onSubmit={submit} noValidate>
      <h1>Konto anlegen</h1>
      {error && (
        <p className="error-box" role="alert">
          {error}
        </p>
      )}
      <label>
        Benutzername
        <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" autoFocus />
      </label>
      <label>
        E-Mail
        <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
      </label>
      <label>
        Passwort <span className="muted">(mindestens 8 Zeichen)</span>
        <input
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="new-password"
        />
      </label>
      <button type="submit" disabled={busy}>
        {busy ? 'Lege an …' : 'Konto anlegen'}
      </button>
      <p className="muted">
        Schon registriert? <Link to="/login">Anmelden</Link>
      </p>
    </form>
  );
}
