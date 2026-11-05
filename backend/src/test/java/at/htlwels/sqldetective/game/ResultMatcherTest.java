package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.cases.CaseModel.ExpectedResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests zu SCRUM-59: Ergebnis statt Wortlaut pruefen (F44, F45).
 */
class ResultMatcherTest {

    private static final ExpectedResult NAMES = new ExpectedResult(
            List.of("first_name", "last_name"),
            List.of(List.of("Max", "Mustermann"), List.of("Anna", "Berger")),
            false);

    private static boolean matches(ExpectedResult expected, List<String> columns, List<?>... rows) {
        @SuppressWarnings("unchecked")
        List<List<Object>> list = Arrays.stream(rows).map(r -> (List<Object>) r).toList();
        return ResultMatcher.matches(expected, columns, list, false);
    }

    @Test
    @DisplayName("Zwei verschieden formulierte Abfragen mit gleichem Ergebnis sind beide richtig")
    void sameResultIsCorrect() {
        // z. B. SELECT first_name, last_name ... und SELECT p.first_name, p.last_name ... JOIN ...
        assertTrue(matches(NAMES, List.of("first_name", "last_name"),
                List.of("Max", "Mustermann"), List.of("Anna", "Berger")));
        assertTrue(matches(NAMES, List.of("first_name", "last_name"),
                List.of("Anna", "Berger"), List.of("Max", "Mustermann")));
    }

    @Test
    @DisplayName("Spaltenreihenfolge und Gross-/Kleinschreibung der Namen sind egal")
    void columnOrderAndCaseDoNotMatter() {
        assertTrue(matches(NAMES, List.of("LAST_NAME", "First_Name"),
                List.of("Mustermann", "Max"), List.of("Berger", "Anna")));
    }

    @Test
    @DisplayName("Zusaetzliche Spalten sind erlaubt, z. B. bei SELECT *")
    void extraColumnsAreAllowed() {
        assertTrue(matches(NAMES, List.of("id", "first_name", "last_name", "role"),
                List.of(1, "Max", "Mustermann", "student"), List.of(2, "Anna", "Berger", "student")));
    }

    @Test
    @DisplayName("Eine fehlende erwartete Spalte ist falsch")
    void missingColumnIsWrong() {
        assertFalse(matches(NAMES, List.of("first_name"), List.of("Max"), List.of("Anna")));
    }

    @Test
    @DisplayName("Fehlende, zusaetzliche und doppelte Zeilen sind falsch")
    void rowsAreComparedAsMultiset() {
        List<String> cols = List.of("first_name", "last_name");
        assertFalse(matches(NAMES, cols, List.of("Max", "Mustermann")));
        assertFalse(matches(NAMES, cols, List.of("Max", "Mustermann"), List.of("Anna", "Berger"),
                List.of("Lisa", "Hofer")));
        assertFalse(matches(NAMES, cols, List.of("Max", "Mustermann"), List.of("Max", "Mustermann")));
    }

    @Test
    @DisplayName("Die Zeilenreihenfolge zaehlt nur, wenn die Mission sie verlangt")
    void orderOnlyWhenRequired() {
        ExpectedResult ordered = new ExpectedResult(NAMES.columns(), NAMES.rows(), true);
        List<String> cols = List.of("first_name", "last_name");

        assertTrue(matches(ordered, cols, List.of("Max", "Mustermann"), List.of("Anna", "Berger")));
        assertFalse(matches(ordered, cols, List.of("Anna", "Berger"), List.of("Max", "Mustermann")));
    }

    @Test
    @DisplayName("Zahlen und Zeitstempel werden vereinheitlicht")
    void valuesAreNormalised() {
        ExpectedResult expected = new ExpectedResult(List.of("n", "seen_at"),
                List.of(List.of(5, "2026-10-02 18:00:00")), false);

        assertTrue(matches(expected, List.of("n", "seen_at"), List.of(new BigDecimal("5.00"), "2026-10-02 18:00:00.0")));
        assertTrue(matches(expected, List.of("n", "seen_at"), List.of(5L, "2026-10-02T18:00:00")));
        assertFalse(matches(expected, List.of("n", "seen_at"), List.of(5, "2026-10-02 18:00:01")));
    }

    @Test
    @DisplayName("NULL ist nur gleich NULL")
    void nullOnlyEqualsNull() {
        ExpectedResult expected = new ExpectedResult(List.of("class_name"), List.of(Arrays.asList((Object) null)), false);

        assertTrue(matches(expected, List.of("class_name"), Arrays.asList((Object) null)));
        assertFalse(matches(expected, List.of("class_name"), List.of("")));
    }

    @Test
    @DisplayName("Bei mehrdeutigen Spaltennamen (SELECT * ueber einen JOIN) wird die passende Spalte gefunden")
    void ambiguousColumnsAreResolved() {
        ExpectedResult expected = new ExpectedResult(List.of("name"), List.of(List.of("Labor 3")), false);

        // persons.name und rooms.name - nur die zweite passt
        assertTrue(matches(expected, List.of("name", "id", "name"), List.of("Max", 3, "Labor 3")));
    }

    @Test
    @DisplayName("Ein abgeschnittenes Ergebnis gilt nie als richtig")
    void truncatedIsNeverCorrect() {
        assertFalse(ResultMatcher.matches(NAMES, List.of("first_name", "last_name"),
                List.of(List.of("Max", "Mustermann"), List.of("Anna", "Berger")), true));
    }
}
