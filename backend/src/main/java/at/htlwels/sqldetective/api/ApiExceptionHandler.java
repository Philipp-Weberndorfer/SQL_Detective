package at.htlwels.sqldetective.api;

import at.htlwels.sqldetective.auth.DuplicateUserException;
import at.htlwels.sqldetective.query.InvalidQueryException;
import at.htlwels.sqldetective.query.QueryTimeoutException;
import at.htlwels.sqldetective.query.SqlExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Wandelt Ausnahmen aller Controller in eine {@link ApiError}-Antwort um.
 *
 * Fehler, die der Spieler verursacht (leere Abfrage, kaputtes JSON, SQL-Fehler),
 * ergeben 4xx mit einer verstaendlichen Meldung. Nur echte Programmfehler
 * ergeben 500.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // ------------------------------------------------------------ Abfragen

    @ExceptionHandler(InvalidQueryException.class)
    public ResponseEntity<ApiError> onInvalidQuery(InvalidQueryException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), "VALIDATION");
    }

    @ExceptionHandler(QueryTimeoutException.class)
    public ResponseEntity<ApiError> onTimeout(QueryTimeoutException e) {
        return error(HttpStatus.REQUEST_TIMEOUT, e.getMessage(), "TIMEOUT");
    }

    @ExceptionHandler(SqlExecutionException.class)
    public ResponseEntity<ApiError> onSqlError(SqlExecutionException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), "SQL");
    }

    // --------------------------------------------------------------- Login

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> onBadCredentials(AuthenticationException e) {
        return error(HttpStatus.UNAUTHORIZED, "Benutzername oder Passwort ist falsch.", "UNAUTHORIZED");
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ApiError> onDuplicateUser(DuplicateUserException e) {
        return error(HttpStatus.CONFLICT, e.getMessage(), "CONFLICT");
    }

    // ------------------------------------------------------ Anfrageformat

    /** Bean Validation, z. B. leeres Feld - die Meldung kommt aus der Annotation. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> onValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getDefaultMessage())
                .findFirst()
                .orElse("Die Anfrage ist unvollstaendig.");
        return error(HttpStatus.BAD_REQUEST, message, "VALIDATION");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> onUnreadable(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, "Die Anfrage ist kein gueltiges JSON.", "BAD_REQUEST");
    }

    // ------------------------------------------------------------- Rest

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> onUnexpected(Exception e) {
        // Spring-eigene Fehler (404, 405, 415 ...) behalten ihren Statuscode.
        if (e instanceof ErrorResponse response) {
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            return error(status, status.getReasonPhrase(), "BAD_REQUEST");
        }
        log.error("Unerwarteter Fehler", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unerwarteter Fehler. Bitte melde das dem Team.", "INTERNAL");
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message, String type) {
        return ResponseEntity.status(status).body(new ApiError(message, type));
    }
}
