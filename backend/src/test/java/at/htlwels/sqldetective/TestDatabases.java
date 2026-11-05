package at.htlwels.sqldetective;

import at.htlwels.sqldetective.cases.CaseLoader;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.stream.Stream;

/**
 * Beide Datenbanken als echte PostgreSQL-16-Container, initialisiert mit genau
 * den Skripten aus db/ - also so, wie sie auch in docker compose laufen.
 *
 * Die Faelle kommen wie im Betrieb ueber den CaseLoader in die Spieldatenbank:
 * alle Faelle aus cases/ plus der kleine Testfall case-900 aus den Testressourcen.
 *
 * Die Container werden einmal je Testlauf gestartet und von allen Tests
 * geteilt. Voraussetzung: Docker laeuft.
 */
public final class TestDatabases {

    public static final String READONLY_USER = "game_readonly";
    public static final String READONLY_PASSWORD = "readonly_test";

    public static final PostgreSQLContainer<?> APP_DB = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("sqldetective_app")
            .withUsername("sqld")
            .withPassword("test")
            .withCopyFileToContainer(MountableFile.forHostPath(projectDir("db", "app")), "/docker-entrypoint-initdb.d/");

    public static final PostgreSQLContainer<?> GAME_DB = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("sqldetective_game")
            .withUsername("sqld")
            .withPassword("test")
            .withEnv("GAME_DB_READONLY_USER", READONLY_USER)
            .withEnv("GAME_DB_READONLY_PASSWORD", READONLY_PASSWORD)
            .withCopyFileToContainer(MountableFile.forHostPath(projectDir("db", "game")), "/docker-entrypoint-initdb.d/");

    /** cases/ des Projekts plus Testfaelle - so sieht der Fallordner in Tests aus. */
    public static final Path CASES_DIR = prepareCasesDir();

    static {
        APP_DB.start();
        GAME_DB.start();
        // auch fuer Tests ohne Spring-Kontext (z. B. ReadonlyUserTest)
        caseLoader().loadAll(CASES_DIR);
    }

    private TestDatabases() {
    }

    /** Fuer @DynamicPropertySource in Spring-Tests. */
    public static void register(DynamicPropertyRegistry registry) {
        registry.add("sqldetective.app-db.url", APP_DB::getJdbcUrl);
        registry.add("sqldetective.app-db.username", APP_DB::getUsername);
        registry.add("sqldetective.app-db.password", APP_DB::getPassword);
        registry.add("sqldetective.game-db.url", GAME_DB::getJdbcUrl);
        registry.add("sqldetective.game-db.username", () -> READONLY_USER);
        registry.add("sqldetective.game-db.password", () -> READONLY_PASSWORD);
        registry.add("sqldetective.game-db.admin-username", GAME_DB::getUsername);
        registry.add("sqldetective.game-db.admin-password", GAME_DB::getPassword);
        registry.add("sqldetective.cases-dir", CASES_DIR::toString);

        // kurzes Zeitlimit, damit der Timeout-Test nicht lange wartet
        registry.add("sqldetective.query.timeout-ms", () -> "1000");
    }

    public static CaseLoader caseLoader() {
        return new CaseLoader(GAME_DB.getJdbcUrl(), GAME_DB.getUsername(), GAME_DB.getPassword(), READONLY_USER);
    }

    /** Direkte Verbindung als Nur-Lese-Benutzer - ohne jede Pruefung der Anwendung. */
    public static Connection connectGameAsReadonly() throws SQLException {
        return DriverManager.getConnection(GAME_DB.getJdbcUrl(), READONLY_USER, READONLY_PASSWORD);
    }

    /** Verbindung als Eigentuemer der Spieldatenbank, nur zum Nachpruefen im Test. */
    public static Connection connectGameAsOwner() throws SQLException {
        return DriverManager.getConnection(GAME_DB.getJdbcUrl(), GAME_DB.getUsername(), GAME_DB.getPassword());
    }

    /** Ordner unter src/test/resources, z. B. test-cases. */
    public static Path testResource(String name) {
        Path fromBackend = Path.of("src", "test", "resources", name);
        Path dir = Files.isDirectory(fromBackend) ? fromBackend : Path.of("backend").resolve(fromBackend);
        return dir.toAbsolutePath().normalize();
    }

    /** Tests laufen im Ordner backend/, aus der IDE manchmal in der Projektwurzel. */
    private static Path projectDir(String first, String... more) {
        Path relative = Path.of(first, more);
        Path fromBackend = Path.of("..").resolve(relative);
        Path dir = Files.isDirectory(fromBackend) ? fromBackend : relative;
        return dir.toAbsolutePath().normalize();
    }

    private static Path prepareCasesDir() {
        try {
            Path dir = Files.createTempDirectory("sqld-cases-");
            copyTree(projectDir("cases"), dir);
            copyTree(testResource("test-cases"), dir);
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> files = Files.walk(from)) {
            for (Path source : files.toList()) {
                Path target = to.resolve(from.relativize(source).toString());
                if (Files.isDirectory(source)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(source, target);
                }
            }
        }
    }
}
