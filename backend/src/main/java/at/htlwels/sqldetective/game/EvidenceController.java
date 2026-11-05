package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.auth.AuthService;
import at.htlwels.sqldetective.cases.CaseRegistry;
import at.htlwels.sqldetective.game.CaseDtos.EvidenceView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Beweisstuecke eines Falls (F39-F41). */
@RestController
@RequestMapping("/api/cases/{caseId}/evidence")
public class EvidenceController {

    private final CaseRegistry cases;
    private final EvidenceService evidence;
    private final AuthService users;

    public EvidenceController(CaseRegistry cases, EvidenceService evidence, AuthService users) {
        this.cases = cases;
        this.evidence = evidence;
        this.users = users;
    }

    @PostMapping("/{evidenceId}")
    @Operation(summary = "Gefundenes Beweisstueck sichern (nur fuer eine eigene, geloeste Mission)")
    @ApiResponse(responseCode = "409", description = "Noch nicht gefunden oder schon gesichert (RULE)")
    public EvidenceView secure(@PathVariable String caseId, @PathVariable String evidenceId, Authentication auth) {
        return evidence.secure(userId(auth), cases.get(caseId), evidenceId);
    }

    private long userId(Authentication auth) {
        return users.getByUsername(auth.getName()).id();
    }
}
