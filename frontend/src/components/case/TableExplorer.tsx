import { useEffect, useState } from 'react';
import { ApiError, api, type TableSummary } from '../../api';

interface Props {
  caseId: string;
  selected: string | null;
  onSelect: (table: string) => void;
}

/** Die Tabellen des aktuellen Falls mit Zeilenanzahl (F20). */
export default function TableExplorer({ caseId, selected, onSelect }: Props) {
  const [tables, setTables] = useState<TableSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .tables(caseId)
      .then(setTables)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Tabellen konnten nicht geladen werden.'));
  }, [caseId]);

  return (
    <nav className="explorer" aria-labelledby="explorer-title">
      <h2 id="explorer-title">Tabellen</h2>
      {error && <p className="muted">{error}</p>}
      {!tables && !error && <p className="muted">Lade …</p>}
      {tables && (
        <ul>
          {tables.map((t) => (
            <li key={t.name}>
              <button
                type="button"
                className={`explorer-table${selected === t.name ? ' selected' : ''}`}
                aria-pressed={selected === t.name}
                onClick={() => onSelect(t.name)}
              >
                <span className="explorer-name">{t.name}</span>
                <span className="explorer-count muted">{t.rowCount} Zeilen</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </nav>
  );
}
