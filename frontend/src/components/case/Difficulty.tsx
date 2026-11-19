/** Schwierigkeit als Sterne, z. B. ★★☆☆ für 2 von 4. */
export default function Difficulty({ value, max = 4 }: { value: number; max?: number }) {
  return (
    <span className="difficulty" aria-label={`Schwierigkeit ${value} von ${max}`} title={`Schwierigkeit ${value} von ${max}`}>
      {'★'.repeat(value)}
      <span className="difficulty-rest">{'☆'.repeat(Math.max(0, max - value))}</span>
    </span>
  );
}
