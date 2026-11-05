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

    /** Eine Tabelle des Falls im Database Explorer (F20). */
    public record TableSummary(String name, long rowCount) {
    }

    /** Aufbau einer Tabelle (F21) mit Fremdschluesseln (F22). */
    public record TableDetail(String name, long rowCount, List<Column> columns) {
    }

    /** @param references Zieltabelle und -spalte eines Fremdschluessels, sonst null */
    public record Column(String name, String type, boolean primaryKey, boolean notNull, Reference references) {
    }

    public record Reference(String table, String column) {
    }

    /** Ein Fall mit dem Fortschritt des angemeldeten Spielers. */
    public record CaseDetail(
            String id,
            String number,
            String title,
            int difficulty,
            String briefing,            // Ausgangslage (F12)
            String goal,                // Ermittlungsziel (F13)
            CaseStatus status,
            String currentMissionId,
            List<String> solvedMissionIds,
            List<MissionView> missions
    ) {
    }

    /** Zustand eines Beweisstuecks fuer den Spieler. */
    public enum EvidenceState {
        SECURED,    // gesichert
        FOUND,      // Mission geloest, noch nicht gesichert
        OPEN        // noch nicht gefunden - nur der Titel geht raus, nie der Text
    }

    /** Ein Beweisstueck; text ist null, solange es nicht gefunden wurde. */
    public record EvidenceView(String id, String title, String text, EvidenceState state) {
    }

    /** Eine Mission mit Nummer, Frage und Zustand fuer den Spieler (F14, F43) - ohne Loesung. */
    public record MissionView(String id, int number, String title, ProgressService.MissionState state) {
    }
}
