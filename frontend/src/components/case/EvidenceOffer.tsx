import { useState } from 'react';
import { ApiError, api, type EvidenceView } from '../../api';

interface Props {
  caseId: string;
  evidence: EvidenceView;
  /** nach dem Sichern, z. B. um das Evidence Board neu zu laden */
  onSecured: () => void;
}

/** Nach einer gelösten Mission: das gefundene Beweisstück mit einem Klick sichern (F39, F40). */
export default function EvidenceOffer({ caseId, evidence, onSecured }: Props) {
  const [secured, setSecured] = useState(evidence.state === 'SECURED');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function secure() {
    setBusy(true);
    setError(null);
    try {
      await api.secureEvidence(caseId, evidence.id);
      setSecured(true);
      onSecured();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Das Beweisstück konnte nicht gesichert werden.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="evidence-offer" aria-label="Neues Beweisstück">
      <p className="evidence-offer-head">Neues Beweisstück: {evidence.title}</p>
      <p className="evidence-text">{evidence.text}</p>
      {secured ? (
        <p className="evidence-secured">✓ Gesichert – du findest es auf dem Evidence Board.</p>
      ) : (
        <button type="button" onClick={secure} disabled={busy}>
          Als Beweis sichern
        </button>
      )}
      {error && <p className="muted">{error}</p>}
    </section>
  );
}
