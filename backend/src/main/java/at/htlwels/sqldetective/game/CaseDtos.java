package at.htlwels.sqldetective.game;

import java.util.List;

/**
 * Oeffentliche Sicht auf die Faelle. Hier stehen bewusst KEINE Loesungsfelder
 * (expectedResult, solution, testQueries, conclusion.correct, requiredEvidence)
 * - nur diese Records gehen an das Frontend (F75, F118).
 */
public final class CaseDtos {

    private CaseDtos() {
    }

    /** Ein Fall mit dem Fortschritt des angemeldeten Spielers. */
    public record CaseDetail(
            String id,
            String title,
            CaseStatus status,
            String currentMissionId,
            List<String> solvedMissionIds
    ) {
    }
}
