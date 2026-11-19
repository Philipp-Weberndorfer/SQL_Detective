package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.auth.AuthService;
import at.htlwels.sqldetective.cases.CaseRegistry;
import at.htlwels.sqldetective.game.CaseDtos.EvidenceBoard;
import at.htlwels.sqldetective.game.CaseDtos.EvidenceState;
import at.htlwels.sqldetective.game.CaseDtos.EvidenceView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping
    @Operation(summary = "Evidence Board: gesicherte und gefundene Beweise mit Text, offene nur mit Titel")
    public EvidenceBoard board(@PathVariable String caseId, Authentication auth) {
        List<EvidenceView> items = evidence.board(userId(auth), cases.get(caseId));
        long secured = items.stream().filter(e -> e.state() == EvidenceState.SECURED).count();
        return new EvidenceBoard(items, secured, items.size());
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
