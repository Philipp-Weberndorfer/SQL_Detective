package at.htlwels.sqldetective.cases;

import at.htlwels.sqldetective.cases.CaseModel.CaseDefinition;
import at.htlwels.sqldetective.cases.CaseModel.Conclusion;
import at.htlwels.sqldetective.cases.CaseModel.Evidence;
import at.htlwels.sqldetective.cases.CaseModel.ExpectedResult;
import at.htlwels.sqldetective.cases.CaseModel.Hint;
import at.htlwels.sqldetective.cases.CaseModel.Mission;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Prueft eine eingelesene case.json gegen das Fallformat, bevor irgendetwas in
 * der Datenbank passiert. Jede Meldung nennt Fall und Feld, damit ein Fehler im
 * Log ohne Suchen auffindbar ist.
 */
final class CaseValidator {

    static final int FORMAT_VERSION = 1;

    static final Pattern CASE_ID = Pattern.compile("case-\\d{3}");

    private CaseValidator() {
    }

    static void validate(CaseDefinition c, String folderName) {
        String where = folderName;

        if (c.formatVersion() != FORMAT_VERSION) {
            throw fail(where, "formatVersion", "muss " + FORMAT_VERSION + " sein, ist " + c.formatVersion());
        }
        if (c.id() == null || !CASE_ID.matcher(c.id()).matches()) {
            throw fail(where, "id", "muss die Form case-001 haben");
        }
        if (!c.id().equals(folderName)) {
            throw fail(where, "id", "muss dem Ordnernamen entsprechen (" + c.id() + ")");
        }
        where = c.id();

        requireText(where, "title", c.title());
        requireText(where, "briefing", c.briefing());
        requireText(where, "goal", c.goal());
        if (c.difficulty() < 1 || c.difficulty() > 4) {
            throw fail(where, "difficulty", "muss zwischen 1 und 4 liegen");
        }
        if (c.focus() == null || c.requires() == null) {
            throw fail(where, c.focus() == null ? "focus" : "requires", "fehlt (leere Liste erlaubt)");
        }

        Set<String> evidenceIds = new HashSet<>();
        List<Evidence> evidence = c.evidence() == null ? List.of() : c.evidence();
        for (int i = 0; i < evidence.size(); i++) {
            Evidence e = evidence.get(i);
            String field = "evidence[" + i + "]";
            requireText(where, field + ".id", e.id());
            requireText(where, field + ".title", e.title());
            requireText(where, field + ".text", e.text());
            if (!evidenceIds.add(e.id())) {
                throw fail(where, field + ".id", "ist doppelt (" + e.id() + ")");
            }
        }

        if (c.missions() == null || c.missions().isEmpty()) {
            throw fail(where, "missions", "fehlt oder ist leer");
        }
        Set<String> missionIds = new HashSet<>();
        for (int i = 0; i < c.missions().size(); i++) {
            Mission m = c.missions().get(i);
            String field = "missions[" + i + "]";
            requireText(where, field + ".id", m.id());
            if (!missionIds.add(m.id())) {
                throw fail(where, field + ".id", "ist doppelt (" + m.id() + ")");
            }
            requireText(where, field + ".title", m.title());
            requireText(where, field + ".solution", m.solution());
            validateExpected(where, field + ".expectedResult", m.expectedResult());
            validateHints(where, field + ".hints", m.hints());
            if (m.evidenceOnSuccess() != null && !evidenceIds.contains(m.evidenceOnSuccess())) {
                throw fail(where, field + ".evidenceOnSuccess", "verweist auf unbekannten Beweis " + m.evidenceOnSuccess());
            }
        }

        validateConclusion(where, c.conclusion(), evidenceIds);
    }

    private static void validateExpected(String where, String field, ExpectedResult r) {
        if (r == null || r.columns() == null || r.columns().isEmpty() || r.rows() == null) {
            throw fail(where, field, "braucht columns und rows");
        }
        for (int i = 0; i < r.rows().size(); i++) {
            if (r.rows().get(i).size() != r.columns().size()) {
                throw fail(where, field + ".rows[" + i + "]", "hat nicht so viele Werte wie columns");
            }
        }
    }

    private static void validateHints(String where, String field, List<Hint> hints) {
        if (hints == null) {
            throw fail(where, field, "fehlt (leere Liste erlaubt)");
        }
        Set<Integer> levels = new HashSet<>();
        for (Hint h : hints) {
            if (h.level() < 1 || h.level() > 4 || !levels.add(h.level())) {
                throw fail(where, field, "Stufen muessen 1 bis 4 sein und duerfen nicht doppelt vorkommen");
            }
            requireText(where, field + ".text", h.text());
        }
    }

    private static void validateConclusion(String where, Conclusion c, Set<String> evidenceIds) {
        if (c == null) {
            throw fail(where, "conclusion", "fehlt");
        }
        requireText(where, "conclusion.question", c.question());
        if (c.options() == null || c.options().size() < 2) {
            throw fail(where, "conclusion.options", "braucht mindestens zwei Antworten");
        }
        if (c.correct() == null || !c.options().contains(c.correct())) {
            throw fail(where, "conclusion.correct", "muss eine der options sein");
        }
        if (c.requiredEvidence() == null || c.requiredEvidence().isEmpty()) {
            throw fail(where, "conclusion.requiredEvidence", "braucht mindestens einen Beweis");
        }
        for (String id : c.requiredEvidence()) {
            if (!evidenceIds.contains(id)) {
                throw fail(where, "conclusion.requiredEvidence", "verweist auf unbekannten Beweis " + id);
            }
        }
        if (c.maxEvidence() != null && c.maxEvidence() < c.requiredEvidence().size()) {
            throw fail(where, "conclusion.maxEvidence", "ist kleiner als die Zahl der geforderten Beweise");
        }
    }

    private static void requireText(String where, String field, String value) {
        if (value == null || value.isBlank()) {
            throw fail(where, field, "fehlt oder ist leer");
        }
    }

    private static CaseFormatException fail(String where, String field, String problem) {
        return new CaseFormatException(where + ": Feld " + field + " " + problem);
    }
}
