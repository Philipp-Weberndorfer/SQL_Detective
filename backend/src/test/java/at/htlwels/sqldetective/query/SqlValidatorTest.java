package at.htlwels.sqldetective.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests zu SCRUM-28 (F31, F32, F111, F126).
 *
 * Das Konzept verlangt ausdruecklich, dass DROP TABLE persons; und
 * DELETE FROM persons; zuverlaessig verhindert werden - diese beiden Faelle
 * stehen deshalb unten als eigener, benannter Test.
 */
class SqlValidatorTest {

    private final SqlValidator validator = new SqlValidator();

    // ------------------------------------------------------------- erlaubt

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM persons",
            "select * from persons",
            "SELECT * FROM access_logs WHERE room_id = 3;",
            "SELECT p.first_name, a.access_time FROM persons p JOIN access_logs a ON p.id = a.person_id",
            "WITH letzte AS (SELECT * FROM device_logins) SELECT * FROM letzte",
            "SELECT COUNT(*) FROM persons GROUP BY role HAVING COUNT(*) > 1",
            "SELECT * FROM persons -- nur ein Kommentar",
            "SELECT 'DROP TABLE persons' AS harmloser_text",
            "SELECT * FROM persons ORDER BY last_name LIMIT 10 OFFSET 5"
    })
    @DisplayName("Lesende Abfragen werden durchgelassen")
    void allowsReadingQueries(String sql) {
        assertDoesNotThrow(() -> validator.validate(sql));
    }

    // ------------------------------------------------------------ verboten

    @Test
    @DisplayName("Die beiden Faelle aus dem Konzept (F126) werden verhindert")
    void blocksTheTwoCasesFromTheConcept() {
        assertThrows(InvalidQueryException.class, () -> validator.validate("DROP TABLE persons;"));
        assertThrows(InvalidQueryException.class, () -> validator.validate("DELETE FROM persons;"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "INSERT INTO persons (first_name) VALUES ('x')",
            "UPDATE persons SET last_name = 'x'",
            "ALTER TABLE persons ADD COLUMN x INT",
            "TRUNCATE persons",
            "CREATE TABLE x (id INT)",
            "GRANT ALL ON persons TO public",
            "COPY persons FROM PROGRAM 'whoami'",
            "DO $$ BEGIN END $$"
    })
    @DisplayName("Schreibende und gefaehrliche Befehle werden abgewiesen")
    void blocksWritingStatements(String sql) {
        assertThrows(InvalidQueryException.class, () -> validator.validate(sql));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT 1; DROP TABLE persons;",
            "SELECT 1;DELETE FROM persons",
            "SELECT 1; SELECT 2"
    })
    @DisplayName("Mehrere Statements in einer Eingabe werden abgewiesen")
    void blocksMultipleStatements(String sql) {
        assertThrows(InvalidQueryException.class, () -> validator.validate(sql));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT 1 --\nDROP TABLE persons",
            "SELECT 1 /* Kommentar */ ; DROP TABLE persons",
            "SELECT /* DROP */ 1; TRUNCATE persons"
    })
    @DisplayName("Kommentare koennen die Pruefung nicht aushebeln")
    void commentsDoNotBypassValidation(String sql) {
        assertThrows(InvalidQueryException.class, () -> validator.validate(sql));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n\t"})
    @DisplayName("Leere Eingaben werden abgewiesen")
    void blocksEmptyInput(String sql) {
        assertThrows(InvalidQueryException.class, () -> validator.validate(sql));
    }

    @Test
    @DisplayName("Ein Kommentar allein ist keine Abfrage")
    void blocksCommentOnly() {
        assertThrows(InvalidQueryException.class, () -> validator.validate("-- nur ein Kommentar"));
    }

    // ------------------------------------------- Hilfsmethode neutralise()

    @Test
    @DisplayName("Zeichenketten und Kommentare werden neutralisiert")
    void neutralisesStringsAndComments() {
        String out = validator.neutralise("SELECT 'DROP TABLE x' -- DELETE\n FROM t");
        org.junit.jupiter.api.Assertions.assertFalse(out.toUpperCase().contains("DROP"));
        org.junit.jupiter.api.Assertions.assertFalse(out.toUpperCase().contains("DELETE"));
        org.junit.jupiter.api.Assertions.assertTrue(out.toUpperCase().contains("FROM T"));
    }
}
