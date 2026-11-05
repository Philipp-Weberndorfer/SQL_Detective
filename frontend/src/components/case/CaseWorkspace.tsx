import { useCallback, useState } from 'react';
import { ApiError, api, type CaseDetail, type QueryResult } from '../../api';
import { useAuth } from '../../auth';
import ResultTable from '../ResultTable';
import SqlEditor from '../SqlEditor';
import TableExplorer from './TableExplorer';

const ERROR_TITLES: Record<string, string> = {
  VALIDATION: 'Abfrage abgewiesen',
  SQL: 'SQL-Fehler',
  TIMEOUT: 'Zeitlimit überschritten',
  NETWORK: 'Keine Verbindung',
};

const isMac = typeof navigator !== 'undefined' && /Mac|iPhone|iPad/.test(navigator.platform);

interface Props {
  detail: CaseDetail;
  onShowBriefing: () => void;
}

/**
 * Arbeitsbereich eines Falls: links der Explorer, in der Mitte Editor und
 * Ergebnis. Jede Funktion ist eine eigene Komponente, die hier nur
 * eingebunden wird.
 */
export default function CaseWorkspace({ detail, onShowBriefing }: Props) {
  const { sessionExpired } = useAuth();
  const [sql, setSql] = useState('SELECT * FROM persons;');
  const [result, setResult] = useState<QueryResult | null>(null);
  const [error, setError] = useState<ApiError | null>(null);
  const [running, setRunning] = useState(false);
  const [table, setTable] = useState<string | null>(null);

  const run = useCallback(async () => {
    if (running || !sql.trim()) {
      return;
    }
    setRunning(true);
    setError(null);
    try {
      setResult(await api.runQuery(detail.id, sql));
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
  }, [running, sql, detail.id, sessionExpired]);

  return (
    <div className="workspace-page">
      <div className="case-header">
        <h1>
          <span className="case-number">#{detail.number}</span> {detail.title}
        </h1>
        <button type="button" className="secondary" onClick={onShowBriefing}>
          Briefing
        </button>
      </div>

      <div className="workspace">
        <TableExplorer caseId={detail.id} selected={table} onSelect={setTable} />

        <section className="workspace-main" aria-label="SQL-Editor und Ergebnis">
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
        </section>
      </div>
    </div>
  );
}
