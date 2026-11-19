package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.ApiTestBase;
import at.htlwels.sqldetective.TestDatabases;
import at.htlwels.sqldetective.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests zu SCRUM-39: Falluebersicht (F09).
 */
class CaseListApiTest extends ApiTestBase {

    @Autowired
    ProgressRepository progress;

    @Autowired
    @Qualifier("appJdbcTemplate")
    JdbcTemplate appDb;

    @Test
    @DisplayName("Alle geladenen Faelle erscheinen mit Nummer, Titel, Schwierigkeit und Schwerpunkt, sortiert")
    void listsAllCasesSorted() throws Exception {
        mvc.perform(get("/api/cases").session(TestUsers.loggedIn(mvc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("case-001"))
                .andExpect(jsonPath("$[0].number").value("001"))
                .andExpect(jsonPath("$[0].title").value("Der verschwundene Laptop"))
                .andExpect(jsonPath("$[0].difficulty").value(1))
                .andExpect(jsonPath("$[0].focus[0]").value("SELECT"))
                // der Testfall aus einem eigenen Ordner taucht ohne Codeaenderung auf
                .andExpect(jsonPath("$[1].id").value("case-900"))
                .andExpect(jsonPath("$[1].title").value("Der Testfall"));
    }

    @Test
    @DisplayName("Der Status kommt aus dem Fortschritt des angemeldeten Spielers: verfuegbar, in Arbeit, geloest")
    void showsTheStatusOfThePlayer() throws Exception {
        String name = TestUsers.uniqueName();
        TestUsers.register(mvc, name);
        MockHttpSession session = TestUsers.login(mvc, name);
        MockHttpSession other = TestUsers.loggedIn(mvc);

        mvc.perform(get("/api/cases").session(session)).andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        mvc.perform(post("/api/cases/case-001/start").with(csrf()).session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/cases").session(session))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].status").value("AVAILABLE"));

        long userId = appDb.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, name);
        progress.markSolved(userId, "case-001");
        mvc.perform(get("/api/cases").session(session)).andExpect(jsonPath("$[0].status").value("SOLVED"));

        // ein anderer Spieler sieht davon nichts
        mvc.perform(get("/api/cases").session(other)).andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("Ohne Anmeldung gibt es keine Falluebersicht")
    void requiresLogin() throws Exception {
        mvc.perform(get("/api/cases")).andExpect(status().isUnauthorized());
    }
}
