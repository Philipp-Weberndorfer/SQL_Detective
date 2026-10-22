package at.htlwels.sqldetective.query;

import at.htlwels.sqldetective.TestDatabases;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests zu SCRUM-25 und zur zweiten Sicherheitsschicht aus SCRUM-28
 * (F33, F108, F109, F112).
 *
 * Diese Tests umgehen die Anwendung bewusst und verbinden sich direkt als
 * game_readonly. Sie weisen nach: Selbst wenn die Validierung versagt, kann
 * dieser Benutzer nichts veraendern und die Anwendungsdatenbank nicht erreichen.
 */
class ReadonlyUserTest {

    @Test
    @DisplayName("Der Nur-Lese-Benutzer kann das Beispielschema lesen")
    void canReadExampleSchema() throws SQLException {
        try (Connection c = TestDatabases.connectGameAsReadonly(); Statement s = c.createStatement()) {
            for (String table : new String[]{"persons", "rooms", "access_logs"}) {
                try (ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    rs.next();
                    assertTrue(rs.getInt(1) > 0, table + " sollte Beispieldaten enthalten");
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "DROP TABLE persons",
            "DELETE FROM persons",
            "UPDATE persons SET last_name = 'x'",
            "INSERT INTO persons (first_name, last_name, role) VALUES ('a', 'b', 'student')",
            "ALTER TABLE persons ADD COLUMN x INT",
            "TRUNCATE persons",
            "CREATE TABLE x (id INT)",
            "CREATE TEMP TABLE x (id INT)",
            "SELECT * INTO kopie FROM persons",
            // GRANT ohne Recht ist in PostgreSQL nur eine Warnung ("no privileges
            // were granted") - deshalb hier Befehle, die sicher scheitern muessen
            "CREATE ROLE angreifer",
            "COMMENT ON TABLE persons IS 'x'"
    })
    @DisplayName("Schreibende Befehle scheitern am Datenbankbenutzer, auch ohne Validierung")
    void writingStatementsFailAtDatabaseLevel(String sql) throws SQLException {
        try (Connection c = TestDatabases.connectGameAsReadonly(); Statement s = c.createStatement()) {
            assertThrows(SQLException.class, () -> s.execute(sql));
        }
        assertPersonsUnchanged();
    }

    @Test
    @DisplayName("Der Nur-Lese-Benutzer erreicht die Anwendungsdatenbank nicht")
    void cannotReachApplicationDatabase() {
        assertThrows(SQLException.class, () -> DriverManager.getConnection(
                TestDatabases.APP_DB.getJdbcUrl(), TestDatabases.READONLY_USER, TestDatabases.READONLY_PASSWORD));
    }

    private static void assertPersonsUnchanged() throws SQLException {
        try (Connection c = TestDatabases.connectGameAsOwner(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM persons WHERE last_name = 'x'")) {
            rs.next();
            assertEquals(0, rs.getInt(1));
        }
    }
}
