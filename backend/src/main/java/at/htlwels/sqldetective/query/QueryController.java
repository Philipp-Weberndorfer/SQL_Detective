package at.htlwels.sqldetective.query;

import at.htlwels.sqldetective.auth.AuthService;
import at.htlwels.sqldetective.cases.CaseModel.LoadedCase;
import at.htlwels.sqldetective.cases.CaseRegistry;
import at.htlwels.sqldetective.game.MissionService;
import org.springframework.security.core.Authentication;
import at.htlwels.sqldetective.query.QueryDtos.QueryRequest;
import at.htlwels.sqldetective.query.QueryDtos.QueryResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpunkt, ueber den der SQL-Editor Abfragen ausfuehrt (F24, F26, F27).
 *
 * Nur fuer angemeldete Benutzer (SecurityConfig). Fehler werden zentral im
 * ApiExceptionHandler in lesbare Antworten umgewandelt.
 *
 * TODO Query-Protokollierung ergaenzen (F120-F122) - Erfolg, Fehlertyp, Laufzeit.
 */
@RestController
@RequestMapping("/api/queries")
public class QueryController {

    private final QueryService queryService;
    private final CaseRegistry cases;
    private final MissionService missions;
    private final AuthService users;

    public QueryController(QueryService queryService, CaseRegistry cases, MissionService missions, AuthService users) {
        this.queryService = queryService;
        this.cases = cases;
        this.missions = missions;
        this.users = users;
    }

    @PostMapping
    @Operation(summary = "SQL-Abfrage gegen die Ermittlungsdatenbank eines Falls ausfuehren",
            description = "Nur eine einzelne lesende Abfrage (SELECT oder WITH) auf die Tabellen des Falls. "
                    + "Hoechstens 500 Zeilen, Abbruch nach dem konfigurierten Zeitlimit.")
    @ApiResponse(responseCode = "200", description = "Spalten und Zeilen, dazu das Ergebnis der Missionspruefung; "
            + "ein leeres Ergebnis ist kein Fehler")
    @ApiResponse(responseCode = "400", description = "Abfrage abgewiesen (VALIDATION) oder von PostgreSQL abgelehnt (SQL)")
    @ApiResponse(responseCode = "401", description = "Nicht angemeldet")
    @ApiResponse(responseCode = "404", description = "Unbekannter Fall")
    @ApiResponse(responseCode = "408", description = "Zeitlimit ueberschritten (TIMEOUT)")
    public QueryResult execute(@Valid @RequestBody QueryRequest request, Authentication auth) {
        LoadedCase c = cases.get(request.caseId());
        QueryResult result = queryService.execute(c.schema(), request.sql());

        // Jede Abfrage im Fall wird automatisch gegen die offene Mission geprueft (F44)
        long userId = users.getByUsername(auth.getName()).id();
        return result.withMission(missions.evaluate(userId, c, result.columns(), result.rows(), result.truncated()));
    }
}
