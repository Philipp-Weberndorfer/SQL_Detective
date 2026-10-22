import { Link } from 'react-router-dom';
import { useAuth } from '../auth';

export default function HomePage() {
  const { user } = useAuth();

  return (
    <section className="hero">
      <h1>Learn SQL by Crime</h1>
      <p className="lead">
        Löse Kriminalfälle, indem du SQL-Abfragen gegen eine Ermittlungsdatenbank schreibst. Ein KI-Detektiv hilft dir
        mit Hinweisen – aber niemals mit der fertigen Lösung.
      </p>
      <div className="actions">
        {user ? (
          <Link to="/editor" className="button">
            Zum SQL-Editor
          </Link>
        ) : (
          <>
            <Link to="/login" className="button">
              Anmelden
            </Link>
            <Link to="/register" className="button secondary">
              Konto anlegen
            </Link>
          </>
        )}
      </div>
    </section>
  );
}
