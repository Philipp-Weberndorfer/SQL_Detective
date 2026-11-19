package at.htlwels.sqldetective.game;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spielfortschritt in der Anwendungsdatenbank (F05, F135): Tabellen
 * case_progress und mission_progress.
 *
 * Gespeichert wird sofort bei jeder Aenderung - es gibt keinen Speichern-Knopf.
 */
@Repository
public class ProgressRepository {

    public record CaseProgress(String caseId, CaseStatus status, String currentMission) {
    }

    public record MissionProgress(String missionId, boolean solved, int attempts) {
    }

    private static final RowMapper<CaseProgress> CASE_MAPPER = (rs, i) -> new CaseProgress(
            rs.getString("case_id"),
            CaseStatus.valueOf(rs.getString("status")),
            rs.getString("current_mission"));

    private static final RowMapper<MissionProgress> MISSION_MAPPER = (rs, i) -> new MissionProgress(
            rs.getString("mission_id"),
            rs.getBoolean("solved"),
            rs.getInt("attempts"));

    private final JdbcTemplate jdbc;

    public ProgressRepository(@Qualifier("appJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CaseProgress> findCase(long userId, String caseId) {
        return jdbc.query("SELECT * FROM case_progress WHERE user_id = ? AND case_id = ?",
                CASE_MAPPER, userId, caseId).stream().findFirst();
    }

    public List<CaseProgress> findCases(long userId) {
        return jdbc.query("SELECT * FROM case_progress WHERE user_id = ?", CASE_MAPPER, userId);
    }

    public List<MissionProgress> findMissions(long userId, String caseId) {
        return jdbc.query("SELECT * FROM mission_progress WHERE user_id = ? AND case_id = ?",
                MISSION_MAPPER, userId, caseId);
    }

    /** Erstes Oeffnen: IN_PROGRESS mit der ersten Mission. Ein zweiter Aufruf aendert nichts. */
    public void startCase(long userId, String caseId, String firstMission) {
        jdbc.update("""
                INSERT INTO case_progress (user_id, case_id, status, current_mission, started_at)
                VALUES (?, ?, 'IN_PROGRESS', ?, NOW())
                ON CONFLICT (user_id, case_id) DO NOTHING
                """, userId, caseId, firstMission);
    }

    /** Zaehlt einen Versuch; eine einmal geloeste Mission bleibt geloest. */
    public void recordAttempt(long userId, String caseId, String missionId, boolean solved) {
        jdbc.update("""
                INSERT INTO mission_progress (user_id, case_id, mission_id, solved, attempts, solved_at)
                VALUES (?, ?, ?, ?, 1, CASE WHEN ? THEN NOW() END)
                ON CONFLICT (user_id, case_id, mission_id) DO UPDATE SET
                    attempts  = mission_progress.attempts + 1,
                    solved    = mission_progress.solved OR EXCLUDED.solved,
                    solved_at = COALESCE(mission_progress.solved_at, EXCLUDED.solved_at)
                """, userId, caseId, missionId, solved, solved);
    }

    /** null = alle Missionen geloest. */
    public void setCurrentMission(long userId, String caseId, String missionId) {
        jdbc.update("""
                UPDATE case_progress SET current_mission = ?, updated_at = NOW()
                WHERE user_id = ? AND case_id = ?
                """, missionId, userId, caseId);
    }

    public void markSolved(long userId, String caseId) {
        jdbc.update("""
                UPDATE case_progress SET status = 'SOLVED', current_mission = NULL, solved_at = NOW(), updated_at = NOW()
                WHERE user_id = ? AND case_id = ?
                """, userId, caseId);
    }
}
