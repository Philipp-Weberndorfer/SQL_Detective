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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests zu SCRUM-59: Mission automatisch pruefen (F44, F45), am Testfall case-900.
 */
class MissionApiTest extends ApiTestBase {

    @Autowired
    @Qualifier("appJdbcTemplate")
    JdbcTemplate appDb;

    private ResultActions query(MockHttpSession session, String sql) throws Exception {
        String body = "{\"caseId\": \"case-900\", \"sql\": \"" + sql.replace("\"", "\\\"") + "\"}";
        return mvc.perform(post("/api/queries").with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Eine falsche Abfrage laesst die Mission offen und verraet nichts ueber die Loesung")
    void wrongQueryKeepsMissionOpen() throws Exception {
        MockHttpSession session = TestUsers.loggedIn(mvc);

        query(session, "SELECT name FROM suspects")
                .andExpect(jsonPath("$.mission.missionId").value("m1"))
                .andExpect(jsonPath("$.mission.solved").value(false))
                .andExpect(jsonPath("$.mission.nextMissionId").value(nullValue()))
                .andExpect(content().string(not(containsString("expected"))))
                .andExpect(content().string(not(containsString("city = 'Wels'"))));
    }

    @Test
    @DisplayName("Die richtige Abfrage loest die Mission und schaltet die naechste frei")
    void correctQuerySolvesMission() throws Exception {
        MockHttpSession session = TestUsers.loggedIn(mvc);

        query(session, "SELECT name FROM suspects WHERE city = 'Wels'")
                .andExpect(jsonPath("$.mission.missionId").value("m1"))
                .andExpect(jsonPath("$.mission.solved").value(true))
                .andExpect(jsonPath("$.mission.nextMissionId").value("m2"))
                .andExpect(jsonPath("$.mission.allSolved").value(false));

        mvc.perform(get("/api/cases/case-900").session(session))
                .andExpect(jsonPath("$.currentMissionId").value("m2"))
                .andExpect(jsonPath("$.solvedMissionIds", contains("m1")));
    }

    @Test
    @DisplayName("Ein anderer Loesungsweg mit gleichem Ergebnis gilt ebenfalls")
    void otherSolutionPathIsAccepted() throws Exception {
        query(TestUsers.loggedIn(mvc), "SELECT s.name, s.city FROM suspects s WHERE s.city LIKE 'W%' ORDER BY s.name DESC")
                .andExpect(jsonPath("$.mission.solved").value(true));
    }

    @Test
    @DisplayName("Bei orderMatters zaehlt die Reihenfolge; nach der letzten Mission ist alles geloest")
    void orderMattersAndLastMission() throws Exception {
        MockHttpSession session = TestUsers.loggedIn(mvc);
        query(session, "SELECT name FROM suspects WHERE city = 'Wels'");

        query(session, "SELECT seen_at, place FROM sightings WHERE suspect_id = 3 ORDER BY seen_at DESC")
                .andExpect(jsonPath("$.mission.missionId").value("m2"))
                .andExpect(jsonPath("$.mission.solved").value(false));

        query(session, "SELECT place, seen_at FROM sightings WHERE suspect_id = 3 ORDER BY seen_at")
                .andExpect(jsonPath("$.mission.solved").value(true))
                .andExpect(jsonPath("$.mission.nextMissionId").value(nullValue()))
                .andExpect(jsonPath("$.mission.allSolved").value(true));

        // danach gibt es nichts mehr zu pruefen
        query(session, "SELECT 1").andExpect(jsonPath("$.mission").value(nullValue()));
    }

    @Test
    @DisplayName("Jeder Versuch wird gezaehlt")
    void attemptsAreCounted() throws Exception {
        String name = TestUsers.uniqueName();
        TestUsers.register(mvc, name);
        MockHttpSession session = TestUsers.login(mvc, name);

        query(session, "SELECT name FROM suspects");
        query(session, "SELECT 1");
        query(session, "SELECT name FROM suspects WHERE city = 'Wels'");

        Integer attempts = appDb.queryForObject("""
                SELECT attempts FROM mission_progress mp JOIN users u ON u.id = mp.user_id
                WHERE u.username = ? AND mp.case_id = 'case-900' AND mp.mission_id = 'm1'
                """, Integer.class, name);
        org.junit.jupiter.api.Assertions.assertEquals(3, attempts);
    }

    @Test
    @DisplayName("Die erste Abfrage oeffnet einen noch nicht begonnenen Fall")
    void firstQueryStartsTheCase() throws Exception {
        MockHttpSession session = TestUsers.loggedIn(mvc);
        query(session, "SELECT 1").andExpect(jsonPath("$.mission.missionId").value("m1"));
        mvc.perform(get("/api/cases/case-900").session(session)).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }
}
