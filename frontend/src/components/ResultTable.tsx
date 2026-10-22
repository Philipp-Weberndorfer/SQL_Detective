import type { QueryResult } from '../api';

/** Ergebnis einer Abfrage als Tabelle mit Spaltenueberschriften. */
export default function ResultTable({ result }: { result: QueryResult }) {
  const { columns, rows, rowCount, truncated, durationMs } = result;

  return (
    <section className="result" aria-label="Ergebnis">
      <p className="result-meta muted">
        {rowCount === 1 ? '1 Zeile' : `${rowCount} Zeilen`} · {durationMs} ms
      </p>
      {truncated && (
        <p className="notice" role="status">
          Das Ergebnis wurde auf {rowCount} Zeilen begrenzt. Grenze deine Abfrage weiter ein, um alles zu sehen.
        </p>
      )}

      {/* Breite Ergebnisse scrollen innerhalb des Rahmens, nicht die ganze Seite. */}
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              {columns.map((col, i) => (
                <th key={i}>{col}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row, r) => (
              <tr key={r}>
                {row.map((cell, c) => (
                  <td key={c}>{cell === null ? <span className="null">NULL</span> : String(cell)}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
        {rowCount === 0 && <p className="empty muted">Keine Zeilen gefunden.</p>}
      </div>
    </section>
  );
}
