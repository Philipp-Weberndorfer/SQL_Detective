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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests zum Database Explorer: Tabellenuebersicht (SCRUM-45, F20).
 */
@SpringBootTest
@AutoConfigureMockMvc
class ExplorerApiTest {

    @DynamicPropertySource
    static void databases(DynamicPropertyRegistry registry) {
        TestDatabases.register(registry);
    }

    @Autowired
    MockMvc mvc;

    @Test
    @DisplayName("Der Explorer listet genau die Tabellen des Falls mit Zeilenanzahl")
    void listsTheTablesOfTheCase() throws Exception {
        mvc.perform(get("/api/cases/case-900/tables").session(TestUsers.loggedIn(mvc)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("sightings", "suspects")))
                .andExpect(jsonPath("$[?(@.name == 'suspects')].rowCount").value(3))
                .andExpect(jsonPath("$[?(@.name == 'sightings')].rowCount").value(4));
    }

    @Test
    @DisplayName("Keine Systemtabellen, keine Tabellen anderer Faelle")
    void onlyTablesOfThisCase() throws Exception {
        mvc.perform(get("/api/cases/case-001/tables").session(TestUsers.loggedIn(mvc)))
                .andExpect(jsonPath("$[*].name", hasItem("persons")))
                .andExpect(jsonPath("$[*].name", not(hasItem("suspects"))))
                .andExpect(jsonPath("$[*].name", not(hasItem("pg_class"))))
                .andExpect(jsonPath("$[*].name", not(hasItem("users"))));
    }

    @Test
    @DisplayName("Ohne Anmeldung gibt es keine Tabellenliste")
    void requiresLogin() throws Exception {
        mvc.perform(get("/api/cases/case-001/tables")).andExpect(status().isUnauthorized());
    }
}
