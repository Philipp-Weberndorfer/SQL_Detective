package at.htlwels.sqldetective.cases;

import at.htlwels.sqldetective.TestDatabases;
import at.htlwels.sqldetective.cases.CaseModel.ColumnInfo;
import at.htlwels.sqldetective.cases.CaseModel.LoadedCase;
import at.htlwels.sqldetective.cases.CaseModel.TableInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests zu SCRUM-44: Fall als Datenpaket (F15-F19, F136).
 */
class CaseLoaderTest {

    private final CaseLoader loader = TestDatabases.caseLoader();

    @Test
    @DisplayName("Alle Faelle aus dem Fallordner werden geladen, jeder in einem eigenen Schema")
    void loadsEveryCaseIntoItsOwnSchema() throws SQLException {
        List<LoadedCase> cases = loader.loadAll(TestDatabases.CASES_DIR);

        assertThat(cases).extracting(LoadedCase::id).contains("case-001", "case-900");
        assertThat(cases).extracting(LoadedCase::schema).contains("case_001", "case_900");
        assertTrue(schemaExists("case_001"));
        assertTrue(schemaExists("case_900"));
    }

    @Test
    @DisplayName("Tabellen werden mit Typ, Schluesseln und Zeilenanzahl beschrieben")
    void describesTables() {
        LoadedCase case001 = byId(loader.loadAll(TestDatabases.CASES_DIR), "case-001");

        TableInfo devices = case001.table("devices").orElseThrow();
        ColumnInfo assignedTo = column(devices, "assigned_to");
        assertEquals("persons", assignedTo.references().table());
        assertEquals("id", assignedTo.references().column());
        assertEquals("varchar(20)", column(devices, "inventory_no").type());
        assertTrue(column(devices, "inventory_no").notNull());

        ColumnInfo personId = column(case001.table("persons").orElseThrow(), "id");
        assertTrue(personId.primaryKey());
        assertEquals("integer", personId.type());
        assertThat(case001.table("persons").orElseThrow().rowCount()).isPositive();

        LoadedCase case900 = byId(loader.loadAll(TestDatabases.CASES_DIR), "case-900");
        assertEquals("timestamp", column(case900.table("sightings").orElseThrow(), "seen_at").type());
        assertEquals(4, case900.table("sightings").orElseThrow().rowCount());
    }

    @Test
    @DisplayName("Erneutes Laden ergibt denselben Stand, ohne Volumes zu loeschen")
    void loadingIsRepeatable() {
        Map<String, Long> first = rowCounts(byId(loader.loadAll(TestDatabases.CASES_DIR), "case-001"));
        Map<String, Long> second = rowCounts(byId(loader.loadAll(TestDatabases.CASES_DIR), "case-001"));
        assertEquals(first, second);
    }

    @Test
    @DisplayName("Ein fehlerhafter Fall wird uebersprungen, die anderen werden trotzdem geladen")
    void brokenCaseIsSkipped(@TempDir Path dir) throws IOException, SQLException {
        copyTestCase(dir, "case-900", UnaryOperator.identity());
        copyTestCase(dir, "case-901", json -> json.replace("\"formatVersion\": 1", "\"formatVersion\": 2"));
        Path broken = copyTestCase(dir, "case-902", UnaryOperator.identity());
        Files.writeString(broken.resolve("seed.sql"), "INSERT INTO gibt_es_nicht VALUES (1);");

        List<LoadedCase> cases = loader.loadAll(dir);

        assertThat(cases).extracting(LoadedCase::id).containsExactly("case-900");
        assertFalse(schemaExists("case_901"));
        assertFalse(schemaExists("case_902"), "ein halb geladener Fall darf nicht zurueckbleiben");
    }

    @Test
    @DisplayName("Formatfehler nennen Fall und Feld")
    void formatErrorsNameCaseAndField(@TempDir Path dir) throws IOException {
        Path folder = copyTestCase(dir, "case-903",
                json -> json.replace("\"solution\": \"SELECT name FROM suspects WHERE city = 'Wels'\",", ""));

        CaseFormatException e = assertThrows(CaseFormatException.class, () -> loader.read(folder));
        assertThat(e.getMessage()).contains("case-903").contains("missions[0].solution");
    }

    @Test
    @DisplayName("Ein neuer Fall-Ordner wird ohne Codeaenderung geladen und ist lesbar")
    void newCaseFolderIsPlayable(@TempDir Path dir) throws IOException, SQLException {
        copyTestCase(dir, "case-905", json -> json.replace("\"Der Testfall\"", "\"Ganz neuer Fall\""));

        LoadedCase loaded = byId(loader.loadAll(dir), "case-905");
        assertEquals("Ganz neuer Fall", loaded.definition().title());

        try (Connection c = TestDatabases.connectGameAsReadonly(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM case_905.suspects")) {
            rs.next();
            assertEquals(3, rs.getInt(1));
        }
    }

    // ------------------------------------------------------------------ Hilfen

    /** Kopiert den Testfall case-900 unter neuer ID und passt case.json an. */
    private static Path copyTestCase(Path dir, String newId, UnaryOperator<String> changeJson) throws IOException {
        Path target = dir.resolve(newId);
        TestDatabases.copyTree(TestDatabases.testResource("test-cases").resolve("case-900"), target);
        Path json = target.resolve("case.json");
        String content = Files.readString(json, StandardCharsets.UTF_8).replace("\"case-900\"", "\"" + newId + "\"");
        Files.writeString(json, changeJson.apply(content), StandardCharsets.UTF_8);
        return target;
    }

    private static LoadedCase byId(List<LoadedCase> cases, String id) {
        return cases.stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow();
    }

    private static ColumnInfo column(TableInfo table, String name) {
        return table.columns().stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow();
    }

    private static Map<String, Long> rowCounts(LoadedCase c) {
        return c.tables().stream().collect(Collectors.toMap(TableInfo::name, TableInfo::rowCount));
    }

    private static boolean schemaExists(String schema) throws SQLException {
        try (Connection c = TestDatabases.connectGameAsOwner(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM pg_namespace WHERE nspname = '" + schema + "'")) {
            rs.next();
            return rs.getInt(1) == 1;
        }
    }
}
