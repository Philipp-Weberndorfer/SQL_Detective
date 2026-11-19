package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.cases.CaseModel.LoadedCase;
import at.htlwels.sqldetective.cases.CaseModel.Mission;
import at.htlwels.sqldetective.game.ProgressRepository.CaseProgress;
import at.htlwels.sqldetective.game.ProgressRepository.MissionProgress;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fortschritt eines Spielers in einem Fall (SCRUM-33).
 *
 * Ohne Eintrag in der Datenbank ist ein Fall AVAILABLE. Die Regeln fuer
 * LOCKED (Voraussetzungen, F10) kommen mit SCRUM-41.
 */
@Service
public class ProgressService {

    /** Zustand einer Mission aus Sicht des Spielers (F14). */
    public enum MissionState { SOLVED, CURRENT, LOCKED }

    /**
     * @param currentMissionId offene Mission, null wenn noch nicht begonnen oder alle geloest
     */
    public record CaseState(CaseStatus status, String currentMissionId, Set<String> solvedMissions) {

        public boolean allMissionsSolved(LoadedCase c) {
            return c.definition().missions().stream().allMatch(m -> solvedMissions.contains(m.id()));
        }

        public MissionState missionState(String missionId) {
            if (solvedMissions.contains(missionId)) {
                return MissionState.SOLVED;
            }
            return missionId.equals(currentMissionId) ? MissionState.CURRENT : MissionState.LOCKED;
        }
    }

    private final ProgressRepository progress;

    public ProgressService(ProgressRepository progress) {
        this.progress = progress;
    }

    public CaseState state(long userId, LoadedCase c) {
        Optional<CaseProgress> row = progress.findCase(userId, c.id());
        if (row.isEmpty()) {
            return new CaseState(CaseStatus.AVAILABLE, null, Set.of());
        }
        Set<String> solved = progress.findMissions(userId, c.id()).stream()
                .filter(MissionProgress::solved)
                .map(MissionProgress::missionId)
                .collect(Collectors.toUnmodifiableSet());
        return new CaseState(row.get().status(), row.get().currentMission(), solved);
    }

    /** Status aller Faelle eines Spielers auf einmal, fuer die Falluebersicht. */
    public CaseStatus status(List<CaseProgress> rows, String caseId) {
        return rows.stream().filter(r -> r.caseId().equals(caseId)).findFirst()
                .map(CaseProgress::status)
                .orElse(CaseStatus.AVAILABLE);
    }

    public List<CaseProgress> allCases(long userId) {
        return progress.findCases(userId);
    }

    /** Erstes Oeffnen eines Falls: AVAILABLE -> IN_PROGRESS mit der ersten Mission. */
    public CaseState start(long userId, LoadedCase c) {
        List<Mission> missions = c.definition().missions();
        progress.startCase(userId, c.id(), missions.get(0).id());
        return state(userId, c);
    }
}
