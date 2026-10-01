package at.htlwels.sqldetective.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Zwei getrennte Datenquellen (F108, F109).
 *
 * appDataSource  - Benutzer, Fortschritt, Spielstand. Voller Schreibzugriff.
 * gameDataSource - Falldaten. Verbindet sich mit dem Nur-Lese-Benutzer und ist
 *                  die EINZIGE Verbindung, ueber die Spieler-SQL laufen darf.
 *
 * Die Trennung ist Absicht: Selbst wenn die Validierung in der Anwendung
 * versagt, kann ueber gameDataSource weder geschrieben noch die
 * Anwendungsdatenbank erreicht werden.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource appDataSource(
            @Value("${sqldetective.app-db.url}") String url,
            @Value("${sqldetective.app-db.username}") String username,
            @Value("${sqldetective.app-db.password}") String password) {

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setPoolName("app-db-pool");
        ds.setMaximumPoolSize(10);
        return ds;
    }

    @Bean
    public DataSource gameDataSource(
            @Value("${sqldetective.game-db.url}") String url,
            @Value("${sqldetective.game-db.username}") String username,
            @Value("${sqldetective.game-db.password}") String password) {

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        ds.setPoolName("game-db-pool");
        ds.setMaximumPoolSize(10);

        // Zusaetzliche Absicherung: Die Verbindung selbst ist schreibgeschuetzt.
        ds.setReadOnly(true);
        return ds;
    }

    @Bean
    public JdbcTemplate appJdbcTemplate(DataSource appDataSource) {
        return new JdbcTemplate(appDataSource);
    }
}
