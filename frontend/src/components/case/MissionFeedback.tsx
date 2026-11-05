import type { MissionOutcome } from '../../api';

/**
 * Rückmeldung der automatischen Missionsprüfung (F44). Bei einem Fehlversuch
 * keine Fehlermeldung, nur der Hinweis, dass die Mission noch offen ist.
 */
export default function MissionFeedback({ outcome }: { outcome: MissionOutcome }) {
  if (outcome.solved) {
    return (
      <div className="success-box" role="status" aria-label="Mission gelöst">
        <strong>✓ Mission gelöst!</strong>{' '}
        {outcome.allSolved ? 'Alle Missionen dieses Falls sind gelöst.' : 'Die nächste Mission ist freigeschaltet.'}
      </div>
    );
  }
  return <p className="mission-open muted">Die Mission ist noch offen.</p>;
}
