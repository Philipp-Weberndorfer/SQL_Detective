import type { MissionView } from '../../api';

const STATE_LABELS: Record<MissionView['state'], { symbol: string; text: string }> = {
  SOLVED: { symbol: '✓', text: 'gelöst' },
  CURRENT: { symbol: '▶', text: 'aktuell' },
  LOCKED: { symbol: '🔒', text: 'gesperrt' },
};

interface Props {
  goal: string;
  missions: MissionView[];
}

/** Ermittlungsziel und alle Ermittlungsfragen mit ihrem Stand (F13, F14). */
export default function InvestigationPanel({ goal, missions }: Props) {
  const solved = missions.filter((m) => m.state === 'SOLVED').length;

  return (
    <section className="panel investigation" aria-labelledby="investigation-title">
      <h2 id="investigation-title">Ermittlung</h2>
      <p className="goal">{goal}</p>
      <p className="muted progress-count">
        {solved} von {missions.length} gelöst
      </p>
      <ol className="mission-list">
        {missions.map((m) => {
          const label = STATE_LABELS[m.state];
          return (
            <li key={m.id} className={`mission-item ${m.state.toLowerCase()}`} aria-current={m.state === 'CURRENT' ? 'step' : undefined}>
              <span className="mission-state" title={label.text} aria-label={label.text}>
                {label.symbol}
              </span>
              <span>{m.title}</span>
            </li>
          );
        })}
      </ol>
    </section>
  );
}
