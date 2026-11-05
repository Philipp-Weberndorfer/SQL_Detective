package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.cases.CaseModel.LoadedCase;
import at.htlwels.sqldetective.cases.CaseModel.Mission;
import at.htlwels.sqldetective.game.ProgressService.CaseState;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Prueft jede Abfrage im Fall automatisch gegen die offene Mission (F44, F45).
 * Einen eigenen Pruefen-Knopf gibt es nicht.
 */
@Service
public class MissionService {

    private final ProgressService progressService;
    private final ProgressRepository progress;

    public MissionService(ProgressService progressService, ProgressRepository progress) {
        this.progressService = progressService;
        this.progress = progress;
    }

    /**
     * @return null, wenn es nichts zu pruefen gibt (Fall geloest oder alle Missionen erledigt)
     */
    public MissionOutcome evaluate(long userId, LoadedCase c, List<String> columns, List<List<Object>> rows,
                                   boolean truncated) {
        CaseState state = progressService.state(userId, c);
        if (state.status() == CaseStatus.AVAILABLE) {
            // die erste Abfrage oeffnet den Fall, falls das Briefing uebersprungen wurde
            state = progressService.start(userId, c);
        }
        if (state.status() == CaseStatus.SOLVED || state.currentMissionId() == null) {
            return null;
        }
        Mission mission = c.definition().mission(state.currentMissionId()).orElse(null);
        if (mission == null) {
            return null;    // Fall wurde inzwischen geaendert und die Mission gibt es nicht mehr
        }

        boolean solved = ResultMatcher.matches(mission.expectedResult(), columns, rows, truncated);
        progress.recordAttempt(userId, c.id(), mission.id(), solved);
        if (!solved) {
            return MissionOutcome.stillOpen(mission.id());
        }

        Set<String> done = new HashSet<>(state.solvedMissions());
        done.add(mission.id());
        String next = c.definition().missions().stream()
                .map(Mission::id)
                .filter(id -> !done.contains(id))
                .findFirst()
                .orElse(null);
        progress.setCurrentMission(userId, c.id(), next);
        return new MissionOutcome(mission.id(), true, next, next == null, EvidenceService.offer(c, mission));
    }
}
