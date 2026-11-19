import { useEffect, useState, type FormEvent } from 'react';
import { ApiError, api, type ConclusionResult, type ConclusionView } from '../../api';

interface Props {
  caseId: string;
  /** auf dem Evidence Board angekreuzte, gesicherte Beweise */
  selectedEvidence: string[];
  /** teilt dem Board mit, wie viele Beweise angekreuzt werden dürfen */
  onMaxEvidence: (max: number) => void;
  onSolved: () => void;
  onCancel: () => void;
}

/**
 * Finale eines Falls: eine Antwort wählen und mit Beweisen belegen
 * (Konzept Kapitel 41). Ob das reicht, entscheidet nur das Backend.
 */
export default function ConclusionPanel({ caseId, selectedEvidence, onMaxEvidence, onSolved, onCancel }: Props) {
  const [view, setView] = useState<ConclusionView | null>(null);
  const [answer, setAnswer] = useState('');
  const [result, setResult] = useState<ConclusionResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api
      .conclusion(caseId)
      .then((v) => {
        setView(v);
        onMaxEvidence(v.maxEvidence);
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Das Finale konnte nicht geladen werden.'));
  }, [caseId, onMaxEvidence]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!answer) {
      setError('Bitte wähle eine Antwort.');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const r = await api.submitConclusion(caseId, answer, selectedEvidence);
      setResult(r);
      if (r.solved) {
        onSolved();
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Die Schlussfolgerung konnte nicht geprüft werden.');
    } finally {
      setBusy(false);
    }
  }

  if (result?.solved) {
    return (
      <section className="conclusion success-box" aria-label="Fall gelöst">
        <h2>Fall gelöst!</h2>
        <p>
          {result.missionsSolved} von {result.missionsTotal} Missionen gelöst · {result.evidenceSecured} von{' '}
          {result.evidenceTotal} Beweisen gesichert
        </p>
      </section>
    );
  }

  return (
    <form className="conclusion panel" aria-label="Fall abschließen" onSubmit={submit}>
      <h2>Fall abschließen</h2>
      {!view && !error && <p className="muted">Lade …</p>}
      {view && (
        <>
          <fieldset>
            <legend>{view.question}</legend>
            {view.options.map((option) => (
              <label key={option} className="conclusion-option">
                <input
                  type="radio"
                  name="answer"
                  value={option}
                  checked={answer === option}
                  onChange={() => setAnswer(option)}
                />
                {option}
              </label>
            ))}
          </fieldset>
          <p className="muted">
            Kreuze auf dem Evidence Board bis zu {view.maxEvidence} gesicherte{' '}
            {view.maxEvidence === 1 ? 'Beweis' : 'Beweise'} an, die deine Antwort belegen. Ausgewählt:{' '}
            {selectedEvidence.length}
          </p>
        </>
      )}
      {result && !result.solved && (
        <p className="notice" role="status">
          {result.message}
        </p>
      )}
      {error && (
        <p className="error-box" role="alert">
          {error}
        </p>
      )}
      <div className="toolbar">
        <button type="submit" disabled={busy || !view}>
          Schlussfolgerung vorlegen
        </button>
        <button type="button" className="secondary" onClick={onCancel}>
          Zurück zur Ermittlung
        </button>
      </div>
    </form>
  );
}
