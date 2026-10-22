package at.htlwels.sqldetective;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Beide Datenbanken als echte PostgreSQL-16-Container, initialisiert mit genau
 * den Skripten aus db/ - also so, wie sie auch in docker compose laufen.
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
            .withCopyFileToContainer(MountableFile.forHostPath(dbScripts("app")), "/docker-entrypoint-initdb.d/");

    public static final PostgreSQLContainer<?> GAME_DB = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("sqldetective_game")
            .withUsername("sqld")
            .withPassword("test")
            .withEnv("GAME_DB_READONLY_USER", READONLY_USER)
            .withEnv("GAME_DB_READONLY_PASSWORD", READONLY_PASSWORD)
            .withCopyFileToContainer(MountableFile.forHostPath(dbScripts("game")), "/docker-entrypoint-initdb.d/");

    static {
        APP_DB.start();
        GAME_DB.start();
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

        // kurzes Zeitlimit, damit der Timeout-Test nicht lange wartet
        registry.add("sqldetective.query.timeout-ms", () -> "1000");
    }

    /** Direkte Verbindung als Nur-Lese-Benutzer - ohne jede Pruefung der Anwendung. */
    public static Connection connectGameAsReadonly() throws SQLException {
        return DriverManager.getConnection(GAME_DB.getJdbcUrl(), READONLY_USER, READONLY_PASSWORD);
    }

    /** Verbindung als Eigentuemer der Spieldatenbank, nur zum Nachpruefen im Test. */
    public static Connection connectGameAsOwner() throws SQLException {
        return DriverManager.getConnection(GAME_DB.getJdbcUrl(), GAME_DB.getUsername(), GAME_DB.getPassword());
    }

    /** Tests laufen im Ordner backend/, aus der IDE manchmal in der Projektwurzel. */
    private static Path dbScripts(String name) {
        Path fromBackend = Path.of("..", "db", name);
        Path dir = Files.isDirectory(fromBackend) ? fromBackend : Path.of("db", name);
        return dir.toAbsolutePath().normalize();
    }
}
