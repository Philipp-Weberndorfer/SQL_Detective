import type { CaseDetail } from '../../api';
import Difficulty from './Difficulty';

interface Props {
  detail: CaseDetail;
  /** true beim ersten Öffnen: Knopf „Ermittlung starten“, sonst „Zurück zur Ermittlung“ */
  firstTime: boolean;
  onContinue: () => void;
  busy?: boolean;
}

/** Einführung in den Fall (F12) – alle Texte kommen aus case.json. */
export default function BriefingView({ detail, firstTime, onContinue, busy = false }: Props) {
  return (
    <article className="card briefing" aria-labelledby="briefing-title">
      <p className="case-number">CASE #{detail.number}</p>
      <h1 id="briefing-title">{detail.title}</h1>
      <Difficulty value={detail.difficulty} />
      <div className="briefing-text">{detail.briefing}</div>
      <button type="button" onClick={onContinue} disabled={busy}>
        {firstTime ? 'Ermittlung starten' : 'Zurück zur Ermittlung'}
      </button>
    </article>
  );
}
