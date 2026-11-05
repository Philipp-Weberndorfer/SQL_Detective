package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.cases.CaseModel.Evidence;
import at.htlwels.sqldetective.cases.CaseModel.LoadedCase;
import at.htlwels.sqldetective.cases.CaseModel.Mission;
import at.htlwels.sqldetective.game.CaseDtos.EvidenceState;
import at.htlwels.sqldetective.game.CaseDtos.EvidenceView;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Beweisstuecke sichern und anzeigen (F39-F41).
 *
 * Ein Beweis gilt als gefunden, sobald der Spieler die Mission geloest hat,
 * die ihn freischaltet (evidenceOnSuccess). Den Text eines noch nicht
 * gefundenen Beweises gibt diese Klasse nie heraus.
 */
@Service
public class EvidenceService {

    private final EvidenceRepository evidence;
    private final ProgressService progress;

    public EvidenceService(EvidenceRepository evidence, ProgressService progress) {
        this.evidence = evidence;
        this.progress = progress;
    }

    /** Alle Beweise des Falls aus Sicht des Spielers - fuer das Evidence Board (F41). */
    public List<EvidenceView> board(long userId, LoadedCase c) {
        Set<String> solved = progress.state(userId, c).solvedMissions();
        Set<String> secured = evidence.securedIds(userId, c.id());
        return c.definition().evidence().stream()
                .map(e -> view(c, e, solved, secured))
                .toList();
    }

    /** Sichert einen gefundenen Beweis - nur fuer eine eigene, geloeste Mission. */
    public EvidenceView secure(long userId, LoadedCase c, String evidenceId) {
        Evidence e = c.definition().evidenceById(evidenceId)
                .orElseThrow(() -> new RuleException("Dieses Beweisstueck gibt es in diesem Fall nicht."));
        Set<String> solved = progress.state(userId, c).solvedMissions();
        if (!found(c, e, solved)) {
            throw new RuleException("Dieses Beweisstueck hast du noch nicht gefunden.");
        }
        if (!evidence.secure(userId, c.id(), evidenceId)) {
            throw new RuleException("Dieses Beweisstueck hast du schon gesichert.");
        }
        return new EvidenceView(e.id(), e.title(), e.text(), EvidenceState.SECURED);
    }

    /** Das Beweisstueck, das eine gerade geloeste Mission freischaltet - zum Anbieten nach dem Loesen. */
    public static EvidenceView offer(LoadedCase c, Mission mission) {
        if (mission.evidenceOnSuccess() == null) {
            return null;
        }
        return c.definition().evidenceById(mission.evidenceOnSuccess())
                .map(e -> new EvidenceView(e.id(), e.title(), e.text(), EvidenceState.FOUND))
                .orElse(null);
    }

    private static EvidenceView view(LoadedCase c, Evidence e, Set<String> solved, Set<String> secured) {
        if (secured.contains(e.id())) {
            return new EvidenceView(e.id(), e.title(), e.text(), EvidenceState.SECURED);
        }
        if (found(c, e, solved)) {
            return new EvidenceView(e.id(), e.title(), e.text(), EvidenceState.FOUND);
        }
        return new EvidenceView(e.id(), e.title(), null, EvidenceState.OPEN);
    }

    private static boolean found(LoadedCase c, Evidence e, Set<String> solvedMissions) {
        return c.definition().missionAwarding(e.id())
                .map(m -> solvedMissions.contains(m.id()))
                .orElse(false);
    }
}
