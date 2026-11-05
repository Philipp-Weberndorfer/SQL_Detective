package at.htlwels.sqldetective.cases;

import at.htlwels.sqldetective.cases.CaseModel.LoadedCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Alle beim Start geladenen Faelle, sortiert nach Fallnummer.
 *
 * Geladen wird im Konstruktor - also beim Hochfahren der Anwendung und
 * bevor die erste Anfrage ankommt. Ein neuer Fall-Ordner ist nach einem
 * Neustart spielbar, ohne dass neu kompiliert werden muss.
 */
@Component
public class CaseRegistry {

    private static final Logger log = LoggerFactory.getLogger(CaseRegistry.class);

    private final Map<String, LoadedCase> cases;

    public CaseRegistry(@Value("${sqldetective.game-db.url}") String url,
                        @Value("${sqldetective.game-db.admin-username}") String adminUser,
                        @Value("${sqldetective.game-db.admin-password}") String adminPassword,
                        @Value("${sqldetective.game-db.username}") String readonlyUser,
                        @Value("${sqldetective.cases-dir:}") String casesDir) {

        Path dir = resolve(casesDir);
        Map<String, LoadedCase> loaded = new LinkedHashMap<>();
        new CaseLoader(url, adminUser, adminPassword, readonlyUser).loadAll(dir)
                .forEach(c -> loaded.put(c.id(), c));
        this.cases = Collections.unmodifiableMap(loaded);
        log.info("{} Fall/Faelle aus {} geladen: {}", cases.size(), dir.toAbsolutePath().normalize(), cases.keySet());
    }

    public Collection<LoadedCase> all() {
        return cases.values();
    }

    public Optional<LoadedCase> find(String caseId) {
        return Optional.ofNullable(caseId).map(cases::get);
    }

    public LoadedCase get(String caseId) {
        return find(caseId).orElseThrow(() -> new CaseNotFoundException(caseId));
    }

    /** Leer = ../cases (Start aus backend/) bzw. cases (Start aus der Projektwurzel). */
    private static Path resolve(String casesDir) {
        if (casesDir != null && !casesDir.isBlank()) {
            return Path.of(casesDir);
        }
        Path fromBackend = Path.of("..", "cases");
        return Files.isDirectory(fromBackend) ? fromBackend : Path.of("cases");
    }
}
