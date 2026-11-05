package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.auth.AuthService;
import at.htlwels.sqldetective.cases.CaseModel.LoadedCase;
import at.htlwels.sqldetective.cases.CaseRegistry;
import at.htlwels.sqldetective.game.CaseDtos.CaseDetail;
import at.htlwels.sqldetective.game.ProgressService.CaseState;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Faelle aus Sicht des angemeldeten Spielers. Jede Antwort enthaelt nur die
 * oeffentlichen Felder aus {@link CaseDtos} - nie Loesungen.
 */
@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseRegistry cases;
    private final ProgressService progress;
    private final AuthService users;

    public CaseController(CaseRegistry cases, ProgressService progress, AuthService users) {
        this.cases = cases;
        this.progress = progress;
        this.users = users;
    }

    @GetMapping("/{caseId}")
    @Operation(summary = "Ein Fall mit dem Fortschritt des angemeldeten Spielers")
    public CaseDetail detail(@PathVariable String caseId, Authentication auth) {
        LoadedCase c = cases.get(caseId);
        return toDetail(c, progress.state(userId(auth), c));
    }

    @PostMapping("/{caseId}/start")
    @Operation(summary = "Fall beginnen: beim ersten Mal AVAILABLE -> IN_PROGRESS, danach ohne Wirkung")
    public CaseDetail start(@PathVariable String caseId, Authentication auth) {
        LoadedCase c = cases.get(caseId);
        return toDetail(c, progress.start(userId(auth), c));
    }

    private CaseDetail toDetail(LoadedCase c, CaseState state) {
        return new CaseDetail(
                c.id(),
                c.definition().title(),
                state.status(),
                state.currentMissionId(),
                c.definition().missions().stream()
                        .map(m -> m.id())
                        .filter(state.solvedMissions()::contains)
                        .toList());
    }

    private long userId(Authentication auth) {
        return users.getByUsername(auth.getName()).id();
    }
}
