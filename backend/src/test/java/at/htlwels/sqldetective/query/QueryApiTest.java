package at.htlwels.sqldetective.query;

import at.htlwels.sqldetective.TestDatabases;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API-Tests zu SCRUM-27, SCRUM-28 und SCRUM-30 gegen echte Datenbanken
 * (F24, F26, F27, F28, F31, F34, F35, F111, F113, F114).
 */
@SpringBootTest
@AutoConfigureMockMvc
class QueryApiTest {

    @DynamicPropertySource
    static void databases(DynamicPropertyRegistry registry) {
        TestDatabases.register(registry);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    @Qualifier("gameDataSource")
    DataSource gameDataSource;

    private ResultActions query(String sql) throws Exception {
        return query("case-001", sql);
    }

    private ResultActions query(String caseId, String sql) throws Exception {
        String body = "{\"caseId\": " + json(caseId) + ", \"sql\": " + json(sql) + "}";
        return mvc.perform(post("/api/queries").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String json(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    // ------------------------------------------------------------ Anmeldung

    @Test
    @DisplayName("Ohne Anmeldung gibt es 401")
    void requiresLogin() throws Exception {
        query("SELECT 1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("UNAUTHORIZED"));
    }

    // ----------------------------------------------------------- Erfolgsfall

    @Test
    @WithMockUser
    @DisplayName("Erfolgsfall liefert Spaltennamen und Zeilen")
    void returnsColumnsAndRows() throws Exception {
        query("SELECT first_name, last_name FROM persons WHERE card_id = 'C-1001'")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[0]").value("first_name"))
                .andExpect(jsonPath("$.columns[1]").value("last_name"))
                .andExpect(jsonPath("$.rows[0][0]").value("Max"))
                .andExpect(jsonPath("$.rows[0][1]").value("Mustermann"))
                .andExpect(jsonPath("$.rowCount").value(1))
                .andExpect(jsonPath("$.truncated").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("Ein leeres Ergebnis ist kein Fehler")
    void emptyResultIsNotAnError() throws Exception {
        query("SELECT * FROM persons WHERE last_name = 'gibt es nicht'")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowCount").value(0))
                .andExpect(jsonPath("$.rows").isEmpty())
                .andExpect(jsonPath("$.columns").isNotEmpty());
    }

    @Test
    @WithMockUser
    @DisplayName("Spielerabfragen laufen als game_readonly")
    void runsAsReadonlyUser() throws Exception {
        query("SELECT current_user")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0][0]").value(TestDatabases.READONLY_USER));
    }

    // ------------------------------------------------------------ Fehlerfall

    @Test
    @WithMockUser
    @DisplayName("SQL-Fehler liefern die PostgreSQL-Meldung, keinen Stacktrace")
    void sqlErrorIsReadable() throws Exception {
        query("SELECT gibts_nicht FROM persons")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("SQL"))
                .andExpect(jsonPath("$.message").value(containsString("gibts_nicht")))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("at org."))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"caseId\": \"case-001\", \"sql\": \"\"}",
            "{\"caseId\": \"case-001\", \"sql\": \"   \"}",
            "{\"caseId\": \"case-001\"}"
    })
    @WithMockUser
    @DisplayName("Leere Abfrage ergibt 400 mit Meldung, nicht 500")
    void emptyQueryIsBadRequest(String body) throws Exception {
        mvc.perform(post("/api/queries").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("Die Abfrage darf nicht leer sein."));
    }

    @Test
    @WithMockUser
    @DisplayName("Kaputtes JSON ergibt 400, nicht 500")
    void malformedJsonIsBadRequest() throws Exception {
        mvc.perform(post("/api/queries").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{sql:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("BAD_REQUEST"));
    }

    // ------------------------------------------------------ Schreibschutz

    @ParameterizedTest
    @ValueSource(strings = {
            "DROP TABLE persons;",
            "DELETE FROM persons;",
            "SELECT 1; DROP TABLE persons;",
            "SELECT $$ -- $$; DROP TABLE persons",
            "SELECT E'\\'' ; DROP TABLE persons; --'"
    })
    @WithMockUser
    @DisplayName("Schreibende Befehle werden mit verstaendlicher Meldung abgewiesen")
    void writingStatementsAreRejected(String sql) throws Exception {
        query(sql)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("VALIDATION"));
        query("SELECT COUNT(*) FROM persons").andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DROP TABLE case_001.persons", "DELETE FROM case_001.persons", "CREATE TEMP TABLE x (id INT)"})
    @DisplayName("Auch an der Validierung vorbei scheitert ein Schreibbefehl ueber die Spielverbindung")
    void gameConnectionRejectsWritesWithoutValidator(String sql) throws SQLException {
        try (Connection c = gameDataSource.getConnection(); Statement s = c.createStatement()) {
            assertThrows(SQLException.class, () -> s.execute(sql));
            c.rollback();
        }
    }

    @Test
    @WithMockUser
    @DisplayName("Sitzungseinstellungen einer Abfrage wirken nicht auf die naechste")
    void sessionSettingsDoNotLeak() throws Exception {
        // ueber alle Verbindungen im Pool hinweg pruefen
        for (int i = 0; i < 12; i++) {
            query("SELECT set_config('application_name', 'manipuliert', false)").andExpect(status().isOk());
        }
        for (int i = 0; i < 12; i++) {
            query("SELECT current_setting('application_name')")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rows[0][0]").value(not("manipuliert")));
        }
    }

    // ------------------------------------------------------------- Faelle

    @Test
    @WithMockUser
    @DisplayName("Ohne Fall ergibt die Abfrage 400 mit Meldung")
    void caseIdIsRequired() throws Exception {
        mvc.perform(post("/api/queries").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sql\": \"SELECT 1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Es ist kein Fall ausgewaehlt."));
    }

    @Test
    @WithMockUser
    @DisplayName("Ein unbekannter Fall ergibt 404")
    void unknownCaseIsNotFound() throws Exception {
        query("case-404", "SELECT 1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("NOT_FOUND"));
    }

    @Test
    @WithMockUser
    @DisplayName("Tabellennamen ohne Schema meinen die Tabellen des gewaehlten Falls")
    void queriesRunAgainstTheChosenCase() throws Exception {
        query("case-900", "SELECT name FROM suspects WHERE city = 'Linz'")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0][0]").value("Bernd"));
        query("case-900", "SELECT * FROM persons")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("SQL"));
        query("case-001", "SELECT COUNT(*) FROM case_001.persons").andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM case_900.suspects",
            "SELECT * FROM \"case_900\".suspects",
            "SELECT p.id FROM persons p JOIN CASE_900 . suspects s ON s.id = p.id"
    })
    @WithMockUser
    @DisplayName("Tabellen anderer Faelle werden abgewiesen")
    void otherCasesAreNotVisible(String sql) throws Exception {
        query("case-001", sql)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("VALIDATION"));
    }

    // ------------------------------------------------------------- Limits

    @Test
    @WithMockUser
    @DisplayName("Lange Abfragen werden nach dem Zeitlimit abgebrochen")
    void longQueryTimesOut() throws Exception {
        query("SELECT pg_sleep(5)")
                .andExpect(status().isRequestTimeout())
                .andExpect(jsonPath("$.type").value("TIMEOUT"))
                .andExpect(jsonPath("$.message").value(containsString("abgebrochen")));
    }

    @Test
    @WithMockUser
    @DisplayName("Ergebnisse werden auf 500 Zeilen begrenzt")
    void resultIsLimitedTo500Rows() throws Exception {
        query("SELECT g FROM generate_series(1, 600) AS g")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowCount").value(500))
                .andExpect(jsonPath("$.truncated").value(true));
    }

    @Test
    @WithMockUser
    @DisplayName("Genau 500 Zeilen gelten nicht als abgeschnitten")
    void exactly500RowsAreNotTruncated() throws Exception {
        query("SELECT g FROM generate_series(1, 500) AS g")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rowCount").value(500))
                .andExpect(jsonPath("$.truncated").value(false));
    }
}
