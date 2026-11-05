import { useCallback, useState } from 'react';
import { ApiError, api, type CaseDetail, type QueryResult } from '../api';
import { useAuth } from '../auth';
import ResultTable from '../components/ResultTable';
import SqlEditor from '../components/SqlEditor';

const ERROR_TITLES: Record<string, string> = {
  VALIDATION: 'Abfrage abgewiesen',
  SQL: 'SQL-Fehler',
  TIMEOUT: 'Zeitlimit überschritten',
  NETWORK: 'Keine Verbindung',
};

const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform);

interface Props {
  caseId: string;
  detail: CaseDetail;
  onShowBriefing: () => void;
}

export default function EditorPage({ caseId, detail, onShowBriefing }: Props) {
  const { sessionExpired } = useAuth();
  const [sql, setSql] = useState('SELECT * FROM persons;');
  const [result, setResult] = useState<QueryResult | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [running, setRunning] = useState(false);

  const run = useCallback(async () => {
    if (running || !sql.trim()) {
      return;
    }
    setRunning(true);
    setError(null);
    try {
      setResult(await api.runQuery(caseId, sql));
    } catch (err) {
      const apiError = err instanceof ApiError ? err : new ApiError('Unerwarteter Fehler.', 'UNKNOWN', 0);
      if (apiError.status === 401) {
        sessionExpired();
        return;
      }
      setResult(null);
      setError(apiError);
    } finally {
      setRunning(false);
    }
  }, [running, sql, caseId, sessionExpired]);

  return (
    <div className="editor-page">
      <div className="case-header">
        <span className="case-header-title">
          <span className="case-number">#{detail.number}</span> {detail.title}
        </span>
        <button type="button" className="secondary" onClick={onShowBriefing}>
          Briefing
        </button>
      </div>
      <h1>Ermittlungsdatenbank</h1>
      <p className="muted">
        Tabellen in Fall #001: persons, rooms, devices, access_logs, device_logins, wifi_connections, lessons,
        witness_statements
      </p>

      <SqlEditor value={sql} onChange={setSql} onRun={run} />

      <div className="toolbar">
        <button type="button" onClick={run} disabled={running || !sql.trim()}>
          {running ? 'Läuft …' : 'Ausführen'}
        </button>
        <span className="muted">oder {isMac ? '⌘' : 'Strg'} + Enter</span>
        {running && (
          <span className="spinner" role="status" aria-label="Abfrage läuft">
            Abfrage läuft …
          </span>
        )}
      </div>

      {error && (
        <div className="error-box" role="alert">
          <strong>{ERROR_TITLES[error.type] ?? 'Fehler'}</strong>
          <pre>{error.message}</pre>
        </div>
      )}

      {result && !error && <ResultTable result={result} />}
    </div>
  );
}
