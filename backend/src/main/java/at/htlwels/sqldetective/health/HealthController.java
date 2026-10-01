package at.htlwels.sqldetective.health;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health-Endpunkt (F137, Akzeptanzkriterium von SCRUM-24).
 *
 * Liefert Status 200 und meldet, ob beide Datenbanken erreichbar sind.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource appDataSource;
    private final DataSource gameDataSource;

    public HealthController(@Qualifier("appDataSource") DataSource appDataSource,
                            @Qualifier("gameDataSource") DataSource gameDataSource) {
        this.appDataSource = appDataSource;
        this.gameDataSource = gameDataSource;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("appDatabase", check(appDataSource));
        body.put("gameDatabase", check(gameDataSource));
        return body;
    }

    private String check(DataSource ds) {
        try (Connection c = ds.getConnection()) {
            return c.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
