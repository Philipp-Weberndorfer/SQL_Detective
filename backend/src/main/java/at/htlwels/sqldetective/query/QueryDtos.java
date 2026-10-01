package at.htlwels.sqldetective.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Datenobjekte fuer den Abfrage-Endpunkt.
 */
public final class QueryDtos {

    private QueryDtos() {
    }

    /** Was der Spieler schickt. */
    public record QueryRequest(
            @NotBlank(message = "Die Abfrage darf nicht leer sein.")
            @Size(max = 10_000, message = "Die Abfrage ist zu lang.")
            String sql
    ) {
    }

    /**
     * Was zurueckkommt.
     *
     * @param columns   Spaltennamen in der Reihenfolge des Ergebnisses
     * @param rows      Zeilen, jede Zelle bereits in eine darstellbare Form gebracht
     * @param rowCount  Anzahl gelieferter Zeilen
     * @param truncated true, wenn wegen des Zeilenlimits abgeschnitten wurde (F35)
     * @param durationMs Laufzeit, wird auch fuer die Lernauswertung protokolliert
     */
    public record QueryResult(
            List<String> columns,
            List<List<Object>> rows,
            int rowCount,
            boolean truncated,
            long durationMs
    ) {
    }

    /** Fehlerantwort - wird dem Spieler im Editor angezeigt (F28). */
    public record QueryError(
            String message,
            String type       // VALIDATION | SQL | TIMEOUT | INTERNAL
    ) {
    }
}
