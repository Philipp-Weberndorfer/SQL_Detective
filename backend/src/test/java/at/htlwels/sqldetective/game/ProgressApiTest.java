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
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests zu SCRUM-33: Spielfortschritt dauerhaft speichern (F05, F135).
 */
class ProgressApiTest extends ApiTestBase {

    @Autowired
    ProgressRepository progress;

    @Autowired
    @Qualifier("appJdbcTemplate")
    JdbcTemplate appDb;

    private ResultActions detail(MockHttpSession session, String caseId) throws Exception {
        return mvc.perform(get("/api/cases/" + caseId).session(session));
    }

    private ResultActions start(MockHttpSession session, String caseId) throws Exception {
        return mvc.perform(post("/api/cases/" + caseId + "/start").with(csrf()).session(session));
    }

    @Test
    @DisplayName("Ohne Anmeldung gibt es 401")
    void requiresLogin() throws Exception {
        mvc.perform(get("/api/cases/case-001")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Ein noch nie geoeffneter Fall ist verfuegbar")
    void unopenedCaseIsAvailable() throws Exception {
        detail(TestUsers.loggedIn(mvc), "case-001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.solvedMissionIds", empty()));
    }

    @Test
    @DisplayName("Beim ersten Oeffnen wird der Fall IN_PROGRESS mit der ersten Mission, danach bleibt er so")
    void firstOpenStartsTheCase() throws Exception {
        MockHttpSession session = TestUsers.loggedIn(mvc);
        start(session, "case-900")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.currentMissionId").value("m1"));
        start(session, "case-900").andExpect(jsonPath("$.currentMissionId").value("m1"));
    }

    @Test
    @DisplayName("Nach Logout und erneutem Login ist der Fortschritt unveraendert")
    void progressSurvivesLogout() throws Exception {
        String name = TestUsers.uniqueName();
        TestUsers.register(mvc, name);
        MockHttpSession first = TestUsers.login(mvc, name);
        start(first, "case-001").andExpect(status().isOk());
        mvc.perform(post("/api/auth/logout").with(csrf()).session(first)).andExpect(status().isNoContent());

        detail(TestUsers.login(mvc, name), "case-001")
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.currentMissionId").value("m1"));
    }

    @Test
    @DisplayName("Jeder Spieler sieht nur seinen eigenen Fortschritt")
    void progressIsPerPlayer() throws Exception {
        MockHttpSession anna = TestUsers.loggedIn(mvc);
        MockHttpSession bernd = TestUsers.loggedIn(mvc);

        start(anna, "case-001").andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        detail(bernd, "case-001").andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("Versuche und geloeste Missionen werden gespeichert; geloest bleibt geloest")
    void attemptsAndSolvedMissionsArePersisted() throws Exception {
        String name = TestUsers.uniqueName();
        TestUsers.register(mvc, name);
        MockHttpSession session = TestUsers.login(mvc, name);
        long userId = appDb.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, name);
        start(session, "case-900");

        progress.recordAttempt(userId, "case-900", "m1", false);
        progress.recordAttempt(userId, "case-900", "m1", true);
        progress.recordAttempt(userId, "case-900", "m1", false);
        progress.setCurrentMission(userId, "case-900", "m2");

        var m1 = progress.findMissions(userId, "case-900").get(0);
        assertEquals(3, m1.attempts());
        assertTrue(m1.solved());
        detail(session, "case-900")
                .andExpect(jsonPath("$.currentMissionId").value("m2"))
                .andExpect(jsonPath("$.solvedMissionIds", contains("m1")));
    }

    @Test
    @DisplayName("Ein unbekannter Fall ergibt 404")
    void unknownCase() throws Exception {
        detail(TestUsers.loggedIn(mvc), "case-404").andExpect(status().isNotFound());
    }
}
