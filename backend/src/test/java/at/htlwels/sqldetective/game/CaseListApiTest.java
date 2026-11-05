package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.TestDatabases;
import at.htlwels.sqldetective.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests zu SCRUM-39: Falluebersicht (F09).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CaseListApiTest {

    @DynamicPropertySource
    static void databases(DynamicPropertyRegistry registry) {
        TestDatabases.register(registry);
    }

    @Autowired
    MockMvc mvc;

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
    @DisplayName("Ohne Anmeldung gibt es keine Falluebersicht")
    void requiresLogin() throws Exception {
        mvc.perform(get("/api/cases")).andExpect(status().isUnauthorized());
    }
}
