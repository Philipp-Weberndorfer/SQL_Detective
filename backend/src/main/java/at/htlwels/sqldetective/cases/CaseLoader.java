package at.htlwels.sqldetective.cases;

import at.htlwels.sqldetective.cases.CaseModel.CaseDefinition;
import at.htlwels.sqldetective.cases.CaseModel.ColumnInfo;
import at.htlwels.sqldetective.cases.CaseModel.ForeignKey;
import at.htlwels.sqldetective.cases.CaseModel.LoadedCase;
import at.htlwels.sqldetective.cases.CaseModel.TableInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Laedt alle Faelle aus dem Fallordner in die Spieldatenbank (F15-F19, F136, AE-10).
 *
 * Jeder Fall bekommt ein eigenes Schema (case-001 -> case_001). Beim Laden wird
 * es komplett neu angelegt - das Laden ist also bei jedem Start wiederholbar,
 * ohne dass Volumes geloescht werden muessen.
 *
 * Dafuer braucht der Loader den Eigentuemer der Spieldatenbank. Diese Verbindung
 * existiert NUR hier, nur waehrend des Ladens, und ist absichtlich keine Bean:
 * Spieler-SQL laeuft weiterhin ausschliesslich ueber game_readonly.
 *
 * Ein fehlerhafter Fall wird mit Meldung uebersprungen; alle anderen werden
 * trotzdem geladen.
 */
public class CaseLoader {

    private static final Logger log = LoggerFactory.getLogger(CaseLoader.class);

    private static final Pattern IDENTIFIER = Pattern.compile("[a-z_][a-z0-9_]*");

    private final String jdbcUrl;
    private final String adminUser;
    private final String adminPassword;
    private final String readonlyUser;

    private final ObjectMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public CaseLoader(String jdbcUrl, String adminUser, String adminPassword, String readonlyUser) {
        if (!IDENTIFIER.matcher(readonlyUser).matches()) {
            throw new IllegalArgumentException("Ungueltiger Name fuer den Nur-Lese-Benutzer: " + readonlyUser);
        }
        this.jdbcUrl = jdbcUrl;
        this.adminUser = adminUser;
        this.adminPassword = adminPassword;
        this.readonlyUser = readonlyUser;
    }

    /** case-001 -> case_001 */
    public static String schemaName(String caseId) {
        return caseId.replace('-', '_');
    }

    public List<LoadedCase> loadAll(Path casesDir) {
        if (!Files.isDirectory(casesDir)) {
            log.warn("Fallordner {} existiert nicht - es werden keine Faelle geladen", casesDir.toAbsolutePath());
            return List.of();
        }

        List<Path> folders;
        try (Stream<Path> list = Files.list(casesDir)) {
            folders = list.filter(Files::isDirectory).sorted().toList();
        } catch (IOException e) {
            throw new IllegalStateException("Fallordner " + casesDir + " ist nicht lesbar", e);
        }

        List<LoadedCase> loaded = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, adminUser, adminPassword)) {
            for (Path folder : folders) {
                try {
                    loaded.add(load(conn, folder));
                } catch (Exception e) {
                    log.error("Fall {} wird uebersprungen: {}", folder.getFileName(), e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Spieldatenbank nicht erreichbar - Faelle koennen nicht geladen werden", e);
        }
        return loaded;
    }

    /** Liest und prueft case.json, ohne die Datenbank anzufassen. */
    public CaseDefinition read(Path folder) throws IOException {
        Path json = folder.resolve("case.json");
        if (!Files.isRegularFile(json)) {
            throw new CaseFormatException(folder.getFileName() + ": case.json fehlt");
        }
        CaseDefinition definition = mapper.readValue(json.toFile(), CaseDefinition.class);
        CaseValidator.validate(definition, folder.getFileName().toString());
        return definition;
    }

    LoadedCase load(Connection conn, Path folder) throws IOException, SQLException {
        CaseDefinition definition = read(folder);
        String schema = schemaName(definition.id());

        Path schemaSql = folder.resolve("schema.sql");
        Path seedSql = folder.resolve("seed.sql");
        for (Path p : List.of(schemaSql, seedSql)) {
            if (!Files.isRegularFile(p)) {
                throw new CaseFormatException(definition.id() + ": " + p.getFileName() + " fehlt");
            }
        }

        boolean autoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (Statement s = conn.createStatement()) {
            s.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
            s.execute("CREATE SCHEMA " + schema);
            // Tabellen ohne Schemaangabe landen damit im Schema des Falls
            s.execute("SET LOCAL search_path TO " + schema);

            ScriptUtils.executeSqlScript(conn, new EncodedResource(new FileSystemResource(schemaSql), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(conn, new EncodedResource(new FileSystemResource(seedSql), StandardCharsets.UTF_8));

            s.execute("GRANT USAGE ON SCHEMA " + schema + " TO " + readonlyUser);
            s.execute("GRANT SELECT ON ALL TABLES IN SCHEMA " + schema + " TO " + readonlyUser);

            List<TableInfo> tables = describe(conn, schema);
            conn.commit();
            log.info("Fall {} geladen: {} Tabellen im Schema {}", definition.id(), tables.size(), schema);
            return new LoadedCase(definition, schema, tables);
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommit);
        }
    }

    /**
     * Beschreibt die Tabellen des Falls fuer den Database Explorer (F20, F21).
     * Die Daten aendern sich nach dem Laden nicht mehr - deshalb wird hier
     * einmal gelesen und spaeter nur noch aus dem Speicher geantwortet.
     */
    private List<TableInfo> describe(Connection conn, String schema) throws SQLException {
        Map<String, List<String[]>> columnsByTable = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT c.relname, a.attname, format_type(a.atttypid, a.atttypmod), a.attnotnull
                FROM pg_attribute a
                JOIN pg_class c ON c.oid = a.attrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = ? AND c.relkind = 'r' AND a.attnum > 0 AND NOT a.attisdropped
                ORDER BY c.relname, a.attnum
                """)) {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    columnsByTable.computeIfAbsent(rs.getString(1), t -> new ArrayList<>())
                            .add(new String[]{rs.getString(2), shortType(rs.getString(3)), String.valueOf(rs.getBoolean(4))});
                }
            }
        }

        DatabaseMetaData meta = conn.getMetaData();
        List<TableInfo> tables = new ArrayList<>();
        for (Map.Entry<String, List<String[]>> entry : columnsByTable.entrySet()) {
            String table = entry.getKey();

            Set<String> primaryKey = new HashSet<>();
            try (ResultSet rs = meta.getPrimaryKeys(null, schema, table)) {
                while (rs.next()) {
                    primaryKey.add(rs.getString("COLUMN_NAME"));
                }
            }
            Map<String, ForeignKey> foreignKeys = new HashMap<>();
            try (ResultSet rs = meta.getImportedKeys(null, schema, table)) {
                while (rs.next()) {
                    foreignKeys.put(rs.getString("FKCOLUMN_NAME"),
                            new ForeignKey(rs.getString("PKTABLE_NAME"), rs.getString("PKCOLUMN_NAME")));
                }
            }

            List<ColumnInfo> columns = entry.getValue().stream()
                    .map(c -> new ColumnInfo(c[0], c[1], primaryKey.contains(c[0]), Boolean.parseBoolean(c[2]),
                            foreignKeys.get(c[0])))
                    .toList();

            long rowCount;
            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery("SELECT count(*) FROM " + schema + "." + quote(table))) {
                rs.next();
                rowCount = rs.getLong(1);
            }
            tables.add(new TableInfo(table, rowCount, columns));
        }
        return tables;
    }

    /** "character varying(50)" -> "varchar(50)" usw., so wie man es selbst schreiben wuerde. */
    static String shortType(String type) {
        return type
                .replace("character varying", "varchar")
                .replace("timestamp without time zone", "timestamp")
                .replace("timestamp with time zone", "timestamptz")
                .replace("time without time zone", "time")
                .replaceFirst("^character\\(", "char(");
    }

    private static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
