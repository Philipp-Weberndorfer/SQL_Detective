package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.ApiTestBase;
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
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests zu SCRUM-60: Fall abschliessen mit Beweisfuehrung, am Testfall case-900
 * (richtig ist Clara, gefordert ist Beweis e2, hoechstens ein Beweis).
 */
class ConclusionApiTest extends ApiTestBase {

    private void query(MockHttpSession session, String sql) throws Exception {
        mvc.perform(post("/api/queries").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"caseId\": \"case-900\", \"sql\": \"" + sql + "\"}"))
                .andExpect(jsonPath("$.mission.solved").value(true));
    }

    /** Spieler, der beide Missionen geloest und die genannten Beweise gesichert hat. */
    private MockHttpSession readyForConclusion(String... secure) throws Exception {
        MockHttpSession session = TestUsers.loggedIn(mvc);
        query(session, "SELECT name FROM suspects WHERE city = 'Wels'");
        query(session, "SELECT seen_at, place FROM sightings WHERE suspect_id = 3 ORDER BY seen_at");
        for (String id : secure) {
            mvc.perform(post("/api/cases/case-900/evidence/" + id).with(csrf()).session(session))
                    .andExpect(status().isOk());
        }
        return session;
    }

    private ResultActions submit(MockHttpSession session, String answer, String evidenceJson) throws Exception {
        return mvc.perform(post("/api/cases/case-900/conclusion").with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"answer\": \"" + answer + "\", \"evidenceIds\": " + evidenceJson + "}"));
    }

    @Test
    @DisplayName("Vor der letzten Mission ist kein Abschluss moeglich")
    void notBeforeAllMissionsAreSolved() throws Exception {
        MockHttpSession session = TestUsers.loggedIn(mvc);
        query(session, "SELECT name FROM suspects WHERE city = 'Wels'");

        mvc.perform(get("/api/cases/case-900/conclusion").session(session))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("RULE"));
        submit(session, "Clara", "[]").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Das Finale zeigt Frage und Antworten, aber weder richtige Antwort noch geforderte Beweise")
    void viewHidesTheSolution() throws Exception {
        mvc.perform(get("/api/cases/case-900/conclusion").session(readyForConclusion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("Wer hat den Schluessel?"))
                .andExpect(jsonPath("$.options.length()").value(3))
                .andExpect(jsonPath("$.maxEvidence").value(1))
                .andExpect(jsonPath("$.correct").doesNotExist())
                .andExpect(jsonPath("$.requiredEvidence").doesNotExist());
    }

    @Test
    @DisplayName("Richtige Antwort mit allen geforderten Beweisen loest den Fall")
    void correctAnswerWithEvidenceSolvesTheCase() throws Exception {
        MockHttpSession session = readyForConclusion("e2");

        submit(session, "Clara", "[\"e2\"]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(true))
                .andExpect(jsonPath("$.missionsSolved").value(2))
                .andExpect(jsonPath("$.missionsTotal").value(2))
                .andExpect(jsonPath("$.evidenceSecured").value(1))
                .andExpect(jsonPath("$.evidenceTotal").value(2));

        mvc.perform(get("/api/cases").session(session))
                .andExpect(jsonPath("$[?(@.id == 'case-900')].status").value("SOLVED"));
    }

    @Test
    @DisplayName("Der richtige Name ohne den geforderten Beweis reicht nicht - und verraet nichts")
    void correctNameWithoutEvidenceIsNotEnough() throws Exception {
        MockHttpSession session = readyForConclusion("e1");

        submit(session, "Clara", "[\"e1\"]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(false))
                .andExpect(content().string(not(containsString("e2"))));
        submit(session, "Clara", "[]").andExpect(jsonPath("$.solved").value(false));

        mvc.perform(get("/api/cases/case-900").session(session)).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("Eine falsche Antwort mit dem richtigen Beweis ist falsch, ein neuer Versuch bleibt moeglich")
    void wrongAnswerFailsButCanRetry() throws Exception {
        MockHttpSession session = readyForConclusion("e2");

        submit(session, "Bernd", "[\"e2\"]")
                .andExpect(jsonPath("$.solved").value(false))
                .andExpect(content().string(not(containsString("Clara"))));
        submit(session, "Clara", "[\"e2\"]").andExpect(jsonPath("$.solved").value(true));
    }

    @Test
    @DisplayName("Nur gesicherte Beweise, hoechstens maxEvidence, und nur vorgegebene Antworten")
    void rulesForSubmission() throws Exception {
        MockHttpSession session = readyForConclusion("e1");

        submit(session, "Clara", "[\"e2\"]").andExpect(status().isConflict());
        submit(session, "Clara", "[\"e1\", \"e2\"]").andExpect(status().isConflict());
        submit(session, "Jemand anderes", "[\"e1\"]").andExpect(status().isConflict());
    }
}
