import type { CaseStatus } from '../../api';

const LABELS: Record<CaseStatus, { symbol: string; text: string }> = {
  LOCKED: { symbol: '🔒', text: 'Gesperrt' },
  AVAILABLE: { symbol: '○', text: 'Verfügbar' },
  IN_PROGRESS: { symbol: '◐', text: 'In Arbeit' },
  SOLVED: { symbol: '✓', text: 'Gelöst' },
};

/** Fallstatus als Text und Symbol – nicht nur über die Farbe erkennbar (F11). */
export default function CaseStatusBadge({ status }: { status: CaseStatus }) {
  const { symbol, text } = LABELS[status];
  return (
    <span className={`status-badge status-${status.toLowerCase()}`}>
      <span aria-hidden="true">{symbol}</span> {text}
    </span>
  );
}
