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

    /** Eine Zeile der Falluebersicht (F09) mit dem Status des Spielers (F11). */
    public record CaseSummary(
            String id,
            String number,          // "001" - fuer die Anzeige als #001
            String title,
            int difficulty,
            List<String> focus,
            CaseStatus status
    ) {
    }

    /** Ein Fall mit dem Fortschritt des angemeldeten Spielers. */
    public record CaseDetail(
            String id,
            String number,
            String title,
            int difficulty,
            String briefing,            // Ausgangslage (F12)
            CaseStatus status,
            String currentMissionId,
            List<String> solvedMissionIds
    ) {
    }
}
