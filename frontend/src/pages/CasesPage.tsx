import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError, api, type CaseSummary } from '../api';
import { useAuth } from '../auth';
import Difficulty from '../components/case/Difficulty';

/** Alle Fälle mit Nummer, Titel, Schwierigkeit und SQL-Schwerpunkt (F09). */
export default function CasesPage() {
  const { sessionExpired } = useAuth();
  const [cases, setCases] = useState<CaseSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .cases()
      .then(setCases)
      .catch((err) => {
        if (err instanceof ApiError && err.status === 401) {
          sessionExpired();
        } else {
          setError(err instanceof ApiError ? err.message : 'Die Fälle konnten nicht geladen werden.');
        }
      });
  }, [sessionExpired]);

  return (
    <section className="cases-page">
      <h1>Fälle</h1>
      <p className="muted">Wähle einen Fall. Jeder Fall trainiert andere SQL-Themen.</p>

      {error && (
        <p className="error-box" role="alert">
          {error}
        </p>
      )}
      {!cases && !error && <p className="muted">Lade …</p>}

      {cases && (
        <ul className="case-list">
          {cases.map((c) => (
            <li key={c.id}>
              <Link to={`/faelle/${c.id}`} className="case-card">
                <span className="case-number">#{c.number}</span>
                <span className="case-title">{c.title}</span>
                <Difficulty value={c.difficulty} />
                <span className="case-focus muted">{c.focus.join(' · ')}</span>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
