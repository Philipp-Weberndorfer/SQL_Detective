import type { ReactNode } from 'react';
import type { MissionView } from '../../api';

interface Props {
  missions: MissionView[];
  /** wird angezeigt, sobald alle Missionen gelöst sind (Weg zum Fallabschluss) */
  whenAllSolved?: ReactNode;
}

/** Die offene Mission direkt über dem Editor (F43). */
export default function MissionBanner({ missions, whenAllSolved }: Props) {
  const current = missions.find((m) => m.state === 'CURRENT');

  if (!current) {
    const allSolved = missions.length > 0 && missions.every((m) => m.state === 'SOLVED');
    return allSolved ? (
      <section className="mission-banner done" aria-label="Aktuelle Mission">
        <p className="mission-title">Alle Missionen sind gelöst.</p>
        {whenAllSolved}
      </section>
    ) : null;
  }

  return (
    <section className="mission-banner" aria-label="Aktuelle Mission">
      <span className="mission-count">
        Mission {current.number} von {missions.length}
      </span>
      <p className="mission-title">{current.title}</p>
    </section>
  );
}
