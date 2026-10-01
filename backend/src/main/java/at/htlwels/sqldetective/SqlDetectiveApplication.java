package at.htlwels.sqldetective;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * Einstiegspunkt des Backends.
 *
 * Die automatische DataSource-Konfiguration ist abgeschaltet, weil wir zwei
 * getrennte Datenquellen brauchen und sie selbst definieren
 * (siehe {@link at.htlwels.sqldetective.config.DataSourceConfig}).
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class SqlDetectiveApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqlDetectiveApplication.class, args);
    }
}
