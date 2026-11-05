package at.htlwels.sqldetective.cases;

import java.util.List;
import java.util.Optional;

/**
 * Abbild von case.json (docs/architektur/fall-format.md).
 *
 * ACHTUNG: Diese Records enthalten die Loesungen (expectedResult, solution,
 * testQueries, conclusion.correct, conclusion.requiredEvidence). Sie verlassen
 * den Server NIE - nach aussen gehen nur die oeffentlichen DTOs (F75, F118).
 */
public final class CaseModel {

    private CaseModel() {
    }

    public record CaseDefinition(
            int formatVersion,
            String id,
            String title,
            int difficulty,             // 1 bis 4 Sterne
            List<String> focus,         // trainierte SQL-Themen
            List<String> requires,      // Fall-IDs, die vorher geloest sein muessen (F10)
            String briefing,
            String goal,
            List<Mission> missions,
            List<Evidence> evidence,
            Conclusion conclusion
    ) {

        public Optional<Mission> mission(String missionId) {
            return missions.stream().filter(m -> m.id().equals(missionId)).findFirst();
        }

        public Optional<Evidence> evidenceById(String evidenceId) {
            return evidence.stream().filter(e -> e.id().equals(evidenceId)).findFirst();
        }

        /** Die Mission, die dieses Beweisstueck freischaltet. */
        public Optional<Mission> missionAwarding(String evidenceId) {
            return missions.stream().filter(m -> evidenceId.equals(m.evidenceOnSuccess())).findFirst();
        }
    }

    public record Mission(
            String id,
            String title,
            List<String> learningGoals,
            ExpectedResult expectedResult,
            List<Hint> hints,
            String solution,
            TestQueries testQueries,
            String evidenceOnSuccess
    ) {
    }

    public record ExpectedResult(
            List<String> columns,
            List<List<Object>> rows,
            boolean orderMatters
    ) {
    }

    public record Hint(int level, String text) {
    }

    /** Zusaetzliche Abfragen fuer die Loesbarkeitstests (F128, F129) - wie Loesungen geheim. */
    public record TestQueries(List<String> correct, List<String> wrong) {
    }

    public record Evidence(String id, String title, String text) {
    }

    public record Conclusion(
            String question,
            List<String> options,
            String correct,
            List<String> requiredEvidence,
            Integer maxEvidence         // wie viele Beweise vorgelegt werden duerfen
    ) {
    }

    /** Ein geladener Fall: Definition, Schema in der Spieldatenbank und Tabellenbeschreibung. */
    public record LoadedCase(CaseDefinition definition, String schema, List<TableInfo> tables) {

        public String id() {
            return definition.id();
        }

        public Optional<TableInfo> table(String name) {
            return tables.stream().filter(t -> t.name().equals(name)).findFirst();
        }
    }

    public record TableInfo(String name, long rowCount, List<ColumnInfo> columns) {
    }

    public record ColumnInfo(String name, String type, boolean primaryKey, boolean notNull, ForeignKey references) {
    }

    public record ForeignKey(String table, String column) {
    }
}
