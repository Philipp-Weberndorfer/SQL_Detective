package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.TestDatabases;
import at.htlwels.sqldetective.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
 * Tests zur Fallansicht: Briefing (SCRUM-42).
 */
@SpringBootTest
@AutoConfigureMockMvc
class CaseDetailApiTest {

    @DynamicPropertySource
    static void databases(DynamicPropertyRegistry registry) {
        TestDatabases.register(registry);
    }

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("Das Briefing kommt mit Nummer, Titel, Schwierigkeit und Ausgangslage aus case.json")
    void briefingComesFromTheCaseFile() throws Exception {
        mvc.perform(get("/api/cases/case-900").session(TestUsers.loggedIn(mvc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number").value("900"))
                .andExpect(jsonPath("$.title").value("Der Testfall"))
                .andExpect(jsonPath("$.difficulty").value(2))
                .andExpect(jsonPath("$.briefing").value("Im Testlabor ist ein Schluessel verschwunden."));
    }

    // ------------------------------------------------- Missionen (SCRUM-58)

    @Test
    @DisplayName("Die Missionen kommen mit Nummer, Frage und Zustand; nach dem Loesen ist die naechste aktuell")
    void missionsWithState() throws Exception {
        MockHttpSession session = TestUsers.loggedIn(mvc);
        mvc.perform(post("/api/cases/case-900/start").with(csrf()).session(session))
                .andExpect(jsonPath("$.missions[0].number").value(1))
                .andExpect(jsonPath("$.missions[0].title").value("Welche Verdaechtigen wohnen in Wels?"))
                .andExpect(jsonPath("$.missions[0].state").value("CURRENT"))
                .andExpect(jsonPath("$.missions[1].state").value("LOCKED"));

        mvc.perform(post("/api/queries").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\": \"case-900\", \"sql\": \"SELECT name FROM suspects WHERE city = 'Wels'\"}"))
                .andExpect(jsonPath("$.mission.solved").value(true));

        // wie nach einem erneuten Login: der Stand kommt aus der Datenbank
        mvc.perform(get("/api/cases/case-900").session(session))
                .andExpect(jsonPath("$.missions[0].state").value("SOLVED"))
                .andExpect(jsonPath("$.missions[1].state").value("CURRENT"))
                .andExpect(jsonPath("$.missions[1].number").value(2));
    }
}
