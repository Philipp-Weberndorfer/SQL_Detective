package at.htlwels.sqldetective.api;

/**
 * Einheitliche Fehlerantwort aller Endpunkte. Die Meldung ist fuer Spieler
 * gedacht und wird im Frontend direkt angezeigt (F28).
 *
 * @param message lesbarer Text, niemals ein Stacktrace
 * @param type    VALIDATION | SQL | TIMEOUT | BAD_REQUEST | UNAUTHORIZED | CONFLICT | INTERNAL
 */
public record ApiError(String message, String type) {
}
