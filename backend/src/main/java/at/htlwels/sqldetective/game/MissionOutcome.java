package at.htlwels.sqldetective.game;

/**
 * Ergebnis der automatischen Missionspruefung einer Abfrage (F44).
 *
 * Bei einer falschen Abfrage steht hier nur, dass die Mission noch offen ist -
 * weder erwartete Werte noch die Referenzloesung.
 *
 * @param nextMissionId die jetzt offene Mission, null wenn alle geloest sind
 */
public record MissionOutcome(
        String missionId,
        boolean solved,
        String nextMissionId,
        boolean allSolved
) {

    static MissionOutcome stillOpen(String missionId) {
        return new MissionOutcome(missionId, false, null, false);
    }
}
