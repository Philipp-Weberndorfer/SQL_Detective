package at.htlwels.sqldetective.game;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.Set;

/** Gesicherte Beweise eines Spielers (Tabelle discovered_evidence, F39, F40). */
@Repository
public class EvidenceRepository {

    private final JdbcTemplate jdbc;

    public EvidenceRepository(@Qualifier("appJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Set<String> securedIds(long userId, String caseId) {
        return new HashSet<>(jdbc.queryForList(
                "SELECT evidence_id FROM discovered_evidence WHERE user_id = ? AND case_id = ?",
                String.class, userId, caseId));
    }

    /** @return false, wenn der Beweis schon gesichert war */
    public boolean secure(long userId, String caseId, String evidenceId) {
        return jdbc.update("""
                INSERT INTO discovered_evidence (user_id, case_id, evidence_id) VALUES (?, ?, ?)
                ON CONFLICT (user_id, case_id, evidence_id) DO NOTHING
                """, userId, caseId, evidenceId) == 1;
    }
}
