package at.htlwels.sqldetective.query;

import at.htlwels.sqldetective.query.QueryDtos.QueryError;
import at.htlwels.sqldetective.query.QueryDtos.QueryRequest;
import at.htlwels.sqldetective.query.QueryDtos.QueryResult;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpunkt, ueber den der SQL-Editor Abfragen ausfuehrt (F24, F26, F27).
 *
 * TODO SCRUM-26: Sobald Login steht, nur fuer angemeldete Benutzer freigeben.
 * TODO Query-Protokollierung ergaenzen (F120-F122) - Erfolg, Fehlertyp, Laufzeit.
 */
@RestController
@RequestMapping("/api/queries")
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping
    public QueryResult execute(@Valid @RequestBody QueryRequest request) {
        return queryService.execute(request.sql());
    }

    @ExceptionHandler(InvalidQueryException.class)
    public ResponseEntity<QueryError> onInvalid(InvalidQueryException e) {
        return ResponseEntity.badRequest()
                .body(new QueryError(e.getMessage(), "VALIDATION"));
    }

    @ExceptionHandler(QueryTimeoutException.class)
    public ResponseEntity<QueryError> onTimeout(QueryTimeoutException e) {
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(new QueryError(e.getMessage(), "TIMEOUT"));
    }

    @ExceptionHandler(SqlExecutionException.class)
    public ResponseEntity<QueryError> onSqlError(SqlExecutionException e) {
        return ResponseEntity.badRequest()
                .body(new QueryError(e.getMessage(), "SQL"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<QueryError> onUnexpected(Exception e) {
        log.error("Unerwarteter Fehler bei einer Abfrage", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new QueryError("Unerwarteter Fehler. Bitte melde das dem Team.", "INTERNAL"));
    }
}
