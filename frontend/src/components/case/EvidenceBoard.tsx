import { useEffect, useState } from 'react';
import { ApiError, api, type EvidenceBoardData, type EvidenceView } from '../../api';

/** Auswahlmodus für den Fallabschluss: gesicherte Beweise ankreuzen. */
export interface EvidenceSelection {
  selected: string[];
  max: number;
  onToggle: (evidenceId: string) => void;
}

interface Props {
  caseId: string;
  /** zählt hoch, wenn sich etwas geändert hat - das Board lädt dann neu */
  version: number;
  onSecured: () => void;
  selection?: EvidenceSelection;
}

const SYMBOLS: Record<EvidenceView['state'], { symbol: string; text: string }> = {
  SECURED: { symbol: '✓', text: 'gesichert' },
  FOUND: { symbol: '!', text: 'gefunden, noch nicht gesichert' },
  OPEN: { symbol: '?', text: 'noch offen' },
};

/** Alle gefundenen und noch offenen Beweise auf einer Tafel (F41). */
export default function EvidenceBoard({ caseId, version, onSecured, selection }: Props) {
  const [board, setBoard] = useState<EvidenceBoardData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .evidence(caseId)
      .then(setBoard)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Das Evidence Board konnte nicht geladen werden.'));
  }, [caseId, version]);

  async function secure(id: string) {
    try {
      await api.secureEvidence(caseId, id);
      onSecured();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Das Beweisstück konnte nicht gesichert werden.');
    }
  }

  return (
    <section className="panel evidence-board" aria-labelledby="board-title">
      <h2 id="board-title">Evidence Board</h2>
      {error && <p className="muted">{error}</p>}
      {!board && !error && <p className="muted">Lade …</p>}
      {board && (
        <>
          <p className="muted progress-count">
            {board.secured} von {board.total} Beweisen gesichert
          </p>
          <ul className="evidence-list">
            {board.items.map((e) => {
              const label = SYMBOLS[e.state];
              const checked = selection?.selected.includes(e.id) ?? false;
              const limitReached = selection ? selection.selected.length >= selection.max && !checked : false;
              return (
                <li key={e.id} className={`evidence-item ${e.state.toLowerCase()}`}>
                  <span className="evidence-state" title={label.text} aria-label={label.text}>
                    {label.symbol}
                  </span>
                  <div className="evidence-body">
                    {selection && e.state === 'SECURED' ? (
                      <label className="evidence-select">
                        <input
                          type="checkbox"
                          checked={checked}
                          disabled={limitReached}
                          onChange={() => selection.onToggle(e.id)}
                        />
                        <strong>{e.title}</strong>
                      </label>
                    ) : (
                      <strong>{e.title}</strong>
                    )}
                    {e.text && <p className="evidence-text">{e.text}</p>}
                    {e.state === 'FOUND' && (
                      <button type="button" className="secondary small" onClick={() => secure(e.id)}>
                        Sichern
                      </button>
                    )}
                  </div>
                </li>
              );
            })}
          </ul>
        </>
      )}
    </section>
  );
}
