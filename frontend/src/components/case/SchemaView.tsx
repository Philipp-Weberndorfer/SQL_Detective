import { useEffect, useState } from 'react';
import { ApiError, api, type TableDetail } from '../../api';

interface Props {
  caseId: string;
  table: string;
  /** Klick auf einen Fremdschlüssel öffnet die Zieltabelle */
  onSelect: (table: string) => void;
}

/** Spalten einer Tabelle mit Datentyp und Schlüsseln (F21, Fremdschlüssel aus F22). */
export default function SchemaView({ caseId, table, onSelect }: Props) {
  const [detail, setDetail] = useState<TableDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setDetail(null);
    setError(null);
    api
      .table(caseId, table)
      .then(setDetail)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Schema konnte nicht geladen werden.'));
  }, [caseId, table]);

  return (
    <section className="panel schema" aria-labelledby="schema-title">
      <h2 id="schema-title">
        Aufbau von <code>{table}</code>
      </h2>
      {error && <p className="muted">{error}</p>}
      {!detail && !error && <p className="muted">Lade …</p>}
      {detail && (
        <ul className="schema-columns">
          {detail.columns.map((c) => (
            <li key={c.name}>
              <span className="schema-name">
                <code>{c.name}</code>
                {c.primaryKey && (
                  <abbr className="key-badge" title="Primärschlüssel">
                    PK
                  </abbr>
                )}
                {c.notNull && !c.primaryKey && (
                  <abbr className="key-badge muted-badge" title="Pflichtfeld (NOT NULL)">
                    NN
                  </abbr>
                )}
              </span>
              <span className="schema-type muted">{c.type}</span>
              {c.references && (
                <button
                  type="button"
                  className="schema-fk"
                  title={`Fremdschlüssel auf ${c.references.table}.${c.references.column}`}
                  onClick={() => onSelect(c.references!.table)}
                >
                  → {c.references.table}.{c.references.column}
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
