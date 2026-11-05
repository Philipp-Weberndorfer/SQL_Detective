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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests zu SCRUM-55: Hinweis als Beweis sichern (F39, F40), am Testfall case-900.
 */
class EvidenceApiTest extends ApiTestBase {

    @Autowired
    @Qualifier("appJdbcTemplate")
    JdbcTemplate appDb;

    private ResultActions solveMission1(MockHttpSession session) throws Exception {
        return mvc.perform(post("/api/queries").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"caseId\": \"case-900\", \"sql\": \"SELECT name FROM suspects WHERE city = 'Wels'\"}"));
    }

    private ResultActions secure(MockHttpSession session, String evidenceId) throws Exception {
        return mvc.perform(post("/api/cases/case-900/evidence/" + evidenceId).with(csrf()).session(session));
    }

    @Test
    @DisplayName("Nach einer geloesten Mission wird das zugehoerige Beweisstueck angeboten")
    void solvedMissionOffersEvidence() throws Exception {
        solveMission1(TestUsers.loggedIn(mvc))
                .andExpect(jsonPath("$.mission.solved").value(true))
                .andExpect(jsonPath("$.mission.evidence.id").value("e1"))
                .andExpect(jsonPath("$.mission.evidence.title").value("Wohnorte"))
                .andExpect(jsonPath("$.mission.evidence.text").value("Anna und Clara wohnen in Wels."))
                .andExpect(jsonPath("$.mission.evidence.state").value("FOUND"));
    }

    @Test
    @DisplayName("Ein gesicherter Beweis wird dauerhaft gespeichert; doppelt sichern geht nicht")
    void securedEvidenceIsStoredOnce() throws Exception {
        String name = TestUsers.uniqueName();
        TestUsers.register(mvc, name);
        MockHttpSession session = TestUsers.login(mvc, name);
        solveMission1(session);

        secure(session, "e1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("SECURED"));
        secure(session, "e1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("RULE"));

        Integer rows = appDb.queryForObject("""
                SELECT COUNT(*) FROM discovered_evidence de JOIN users u ON u.id = de.user_id
                WHERE u.username = ? AND de.case_id = 'case-900' AND de.evidence_id = 'e1'
                """, Integer.class, name);
        assertEquals(1, rows);
    }

    @Test
    @DisplayName("Ein Beweis einer noch nicht geloesten Mission laesst sich nicht sichern, auch nicht per API")
    void unsolvedMissionCannotBeSecured() throws Exception {
        MockHttpSession session = TestUsers.loggedIn(mvc);
        secure(session, "e1").andExpect(status().isConflict()).andExpect(jsonPath("$.type").value("RULE"));

        solveMission1(session);
        secure(session, "e2").andExpect(status().isConflict());
    }

    // ----------------------------------------------- Evidence Board (SCRUM-56)

    @Test
    @DisplayName("Das Board zeigt offene Beweise nur mit Titel, gefundene und gesicherte mit Text")
    void boardShowsStates() throws Exception {
        MockHttpSession session = TestUsers.loggedIn(mvc);

        mvc.perform(get("/api/cases/case-900/evidence").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.secured").value(0))
                .andExpect(jsonPath("$.items[0].title").value("Wohnorte"))
                .andExpect(jsonPath("$.items[0].state").value("OPEN"))
                .andExpect(jsonPath("$.items[0].text").value(nullValue()));

        solveMission1(session);
        mvc.perform(get("/api/cases/case-900/evidence").session(session))
                .andExpect(jsonPath("$.items[0].state").value("FOUND"))
                .andExpect(jsonPath("$.items[0].text").value("Anna und Clara wohnen in Wels."))
                .andExpect(jsonPath("$.items[1].state").value("OPEN"));

        secure(session, "e1");
        mvc.perform(get("/api/cases/case-900/evidence").session(session))
                .andExpect(jsonPath("$.items[0].state").value("SECURED"))
                .andExpect(jsonPath("$.secured").value(1));
    }

    @Test
    @DisplayName("Was ein anderer Spieler gefunden hat, kann man nicht sichern")
    void otherPlayersEvidenceIsNotMine() throws Exception {
        MockHttpSession anna = TestUsers.loggedIn(mvc);
        solveMission1(anna);
        secure(anna, "e1").andExpect(status().isOk());

        secure(TestUsers.loggedIn(mvc), "e1").andExpect(status().isConflict());
    }
}
