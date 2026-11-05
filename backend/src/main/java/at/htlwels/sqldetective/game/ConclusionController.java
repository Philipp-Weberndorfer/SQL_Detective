package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.auth.AuthService;
import at.htlwels.sqldetective.cases.CaseRegistry;
import at.htlwels.sqldetective.game.CaseDtos.ConclusionRequest;
import at.htlwels.sqldetective.game.CaseDtos.ConclusionResult;
import at.htlwels.sqldetective.game.CaseDtos.ConclusionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Finale eines Falls: Schlussfolgerung waehlen und mit Beweisen belegen. */
@RestController
@RequestMapping("/api/cases/{caseId}/conclusion")
public class ConclusionController {

    private final CaseRegistry cases;
    private final ConclusionService conclusions;
    private final AuthService users;

    public ConclusionController(CaseRegistry cases, ConclusionService conclusions, AuthService users) {
        this.cases = cases;
        this.conclusions = conclusions;
        this.users = users;
    }

    @GetMapping
    @Operation(summary = "Frage und Antwortmoeglichkeiten des Finales (erst wenn alle Missionen geloest sind)")
    @ApiResponse(responseCode = "409", description = "Noch nicht alle Missionen geloest (RULE)")
    public ConclusionView view(@PathVariable String caseId, Authentication auth) {
        return conclusions.view(userId(auth), cases.get(caseId));
    }

    @PostMapping
    @Operation(summary = "Schlussfolgerung mit gesicherten Beweisen vorlegen",
            description = "Geloest ist der Fall nur mit richtiger Antwort und allen geforderten Beweisen.")
    @ApiResponse(responseCode = "409", description = "Noch nicht alle Missionen geloest, ungueltige Antwort, "
            + "nicht gesicherte oder zu viele Beweise (RULE)")
    public ConclusionResult submit(@PathVariable String caseId, @Valid @RequestBody ConclusionRequest request,
                                   Authentication auth) {
        return conclusions.submit(userId(auth), cases.get(caseId), request.answer(), request.evidenceIds());
    }

    private long userId(Authentication auth) {
        return users.getByUsername(auth.getName()).id();
    }
}
