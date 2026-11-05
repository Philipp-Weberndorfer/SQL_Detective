package at.htlwels.sqldetective;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Basis fuer alle API-Tests gegen die Testcontainer-Datenbanken.
 *
 * Alle Unterklassen teilen sich EINEN Spring-Kontext (gleiche Konfiguration =
 * gleicher Cache-Schluessel). Mit einem eigenen Kontext je Testklasse haette
 * jede Klasse zwei Verbindungspools offen gehalten - bei vielen Testklassen
 * reicht max_connections von PostgreSQL dann nicht mehr.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class ApiTestBase {

    @DynamicPropertySource
    static void databases(DynamicPropertyRegistry registry) {
        TestDatabases.register(registry);
    }

    @Autowired
    protected MockMvc mvc;
}
