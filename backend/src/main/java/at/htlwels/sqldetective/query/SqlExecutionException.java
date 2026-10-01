package at.htlwels.sqldetective.query;

/**
 * Die Datenbank hat die Abfrage abgelehnt - Syntaxfehler, unbekannte Spalte
 * oder fehlende Rechte. Die Originalmeldung von PostgreSQL wird bewusst
 * weitergereicht, weil sie Teil des Lerneffekts ist (F28).
 */
public class SqlExecutionException extends RuntimeException {

    public SqlExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
