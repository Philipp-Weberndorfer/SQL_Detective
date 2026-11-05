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
}
