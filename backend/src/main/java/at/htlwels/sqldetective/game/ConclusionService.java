package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.cases.CaseModel.Conclusion;
import at.htlwels.sqldetective.cases.CaseModel.LoadedCase;
import at.htlwels.sqldetective.game.CaseDtos.ConclusionResult;
import at.htlwels.sqldetective.game.CaseDtos.ConclusionView;
import at.htlwels.sqldetective.game.ProgressService.CaseState;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Fall abschliessen mit Beweisfuehrung (Konzept Kapitel 41, Kapitel 43 Schritt 6).
 *
 * Bewertet werden Antwort UND Beweise - der richtige Name allein reicht nicht.
 * Geprueft wird ausschliesslich hier; conclusion.correct und requiredEvidence
 * verlassen den Server nie, auch nicht in einer Fehlermeldung.
 */
@Service
public class ConclusionService {

    static final String NOT_CONVINCING =
            "Deine Beweisfuehrung ueberzeugt noch nicht. Pruefe Antwort und Beweise und versuche es erneut.";

    private final ProgressService progressService;
    private final ProgressRepository progress;
    private final EvidenceRepository evidence;

    public ConclusionService(ProgressService progressService, ProgressRepository progress, EvidenceRepository evidence) {
        this.progressService = progressService;
        this.progress = progress;
        this.evidence = evidence;
    }

    public ConclusionView view(long userId, LoadedCase c) {
        CaseState state = requireAllMissionsSolved(userId, c);
        Conclusion conclusion = c.definition().conclusion();
        return new ConclusionView(conclusion.question(), conclusion.options(), maxEvidence(conclusion),
                state.status() == CaseStatus.SOLVED);
    }

    public ConclusionResult submit(long userId, LoadedCase c, String answer, List<String> evidenceIds) {
        CaseState state = requireAllMissionsSolved(userId, c);
        Conclusion conclusion = c.definition().conclusion();

        if (state.status() == CaseStatus.SOLVED) {
            return result(userId, c, true, "Diesen Fall hast du schon geloest.");
        }
        if (!conclusion.options().contains(answer)) {
            throw new RuleException("Bitte waehle eine der vorgegebenen Antworten.");
        }
        Set<String> presented = new LinkedHashSet<>(evidenceIds);
        if (presented.size() > maxEvidence(conclusion)) {
            throw new RuleException("Du kannst hoechstens " + maxEvidence(conclusion) + " Beweise vorlegen.");
        }
        if (!evidence.securedIds(userId, c.id()).containsAll(presented)) {
            throw new RuleException("Du kannst nur Beweise vorlegen, die du gesichert hast.");
        }

        boolean convincing = conclusion.correct().equals(answer) && presented.containsAll(conclusion.requiredEvidence());
        if (!convincing) {
            return result(userId, c, false, NOT_CONVINCING);
        }
        progress.markSolved(userId, c.id());
        return result(userId, c, true, "Fall geloest!");
    }

    private CaseState requireAllMissionsSolved(long userId, LoadedCase c) {
        CaseState state = progressService.state(userId, c);
        if (!state.allMissionsSolved(c)) {
            throw new RuleException("Den Fall kannst du erst abschliessen, wenn alle Missionen geloest sind.");
        }
        return state;
    }

    private ConclusionResult result(long userId, LoadedCase c, boolean solved, String message) {
        int missionsTotal = c.definition().missions().size();
        int missionsSolved = progressService.state(userId, c).solvedMissions().size();
        long secured = evidence.securedIds(userId, c.id()).size();
        return new ConclusionResult(solved, message, missionsSolved, missionsTotal, secured,
                c.definition().evidence().size());
    }

    private static int maxEvidence(Conclusion conclusion) {
        return conclusion.maxEvidence() != null ? conclusion.maxEvidence() : conclusion.requiredEvidence().size();
    }
}
