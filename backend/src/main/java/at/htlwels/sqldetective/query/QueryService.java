package at.htlwels.sqldetective.query;

import at.htlwels.sqldetective.query.QueryDtos.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Fuehrt die Abfragen der Spieler aus (F26, F27, F34, F35).
 *
 * Verwendet ausschliesslich die gameDataSource, also den Nur-Lese-Benutzer.
 * Die Verbindung zur Anwendungsdatenbank wird hier bewusst nicht injiziert -
 * so kann an dieser Stelle gar kein Zugriff darauf entstehen.
 */
@Service
public class QueryService {

    private static final Logger log = LoggerFactory.getLogger(QueryService.class);

    private final DataSource gameDataSource;
    private final SqlValidator validator;
    private final int timeoutSeconds;
    private final int maxRows;

    public QueryService(@Qualifier("gameDataSource") DataSource gameDataSource,
                        SqlValidator validator,
                        @Value("${sqldetective.query.timeout-ms}") int timeoutMs,
                        @Value("${sqldetective.query.max-rows}") int maxRows) {
        this.gameDataSource = gameDataSource;
        this.validator = validator;
        this.timeoutSeconds = Math.max(1, (int) Math.ceil(timeoutMs / 1000.0));
        this.maxRows = maxRows;
    }

    public QueryResult execute(String sql) {
        validator.validate(sql);

        long start = System.nanoTime();

        try (Connection conn = gameDataSource.getConnection()) {

            // Dritte Absicherung auf Verbindungsebene. PostgreSQL weist
            // schreibende Befehle in einer read-only Transaktion ab.
            conn.setReadOnly(true);

            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(timeoutSeconds);   // F34, F113
                stmt.setMaxRows(maxRows + 1);           // +1, um Abschneiden zu erkennen

                try (ResultSet rs = stmt.executeQuery(sql)) {
                    return read(rs, System.nanoTime() - start);
                }
            }

        } catch (SQLTimeoutException e) {
            throw new QueryTimeoutException(
                    "Die Abfrage hat laenger als " + timeoutSeconds + " Sekunden gebraucht und wurde abgebrochen. "
                            + "Versuche, das Ergebnis staerker einzugrenzen.");
        } catch (SQLException e) {
            log.debug("SQL-Fehler einer Spielerabfrage: {}", e.getMessage());
            // Die PostgreSQL-Meldung wird bewusst durchgereicht - sie ist Teil
            // des Lerneffekts (F28). Der AI Error Coach erklaert sie spaeter.
            throw new SqlExecutionException(e.getMessage(), e);
        }
    }

    private QueryResult read(ResultSet rs, long elapsedNanos) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();

        List<String> columns = new ArrayList<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columns.add(meta.getColumnLabel(i));
        }

        List<List<Object>> rows = new ArrayList<>();
        boolean truncated = false;

        while (rs.next()) {
            if (rows.size() == maxRows) {
                truncated = true;      // es gaebe mindestens eine Zeile mehr
                break;
            }
            List<Object> row = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                Object value = rs.getObject(i);
                row.add(value == null ? null : asDisplayable(value));
            }
            rows.add(row);
        }

        return new QueryResult(columns, rows, rows.size(), truncated,
                elapsedNanos / 1_000_000);
    }

    /**
     * Typen, die Jackson nicht sinnvoll serialisiert (Arrays, geometrische
     * Typen, intervals), werden als Text ausgegeben. Zahlen, Text, Zeitstempel
     * und Wahrheitswerte bleiben erhalten.
     */
    private Object asDisplayable(Object value) {
        if (value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        if (value instanceof java.sql.Timestamp || value instanceof java.sql.Date
                || value instanceof java.sql.Time) {
            return value.toString();
        }
        return value.toString();
    }
}
