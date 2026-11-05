package at.htlwels.sqldetective.game;

import at.htlwels.sqldetective.TestDatabases;
import at.htlwels.sqldetective.TestUsers;
import at.htlwels.sqldetective.cases.CaseModel.LoadedCase;
import at.htlwels.sqldetective.cases.CaseModel.Mission;
import at.htlwels.sqldetective.cases.CaseRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Kein Fall-Endpunkt liefert Loesungen aus (SCRUM-44, F75, F118, fall-format.md Regel 1).
 *
 * Geprueft wird fuer jeden geladenen Fall und einen frischen Spieler:
 *   - kein JSON-Feld mit einem Loesungsnamen (solution, expectedResult, ...)
 *   - keine Referenzloesung und keine Testabfrage im Antworttext
 *   - kein Text eines Beweises, den der Spieler noch nicht gefunden hat
 *
 * Neue Fall-Endpunkte werden in {@link #endpoints(LoadedCase)} ergaenzt.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CaseSecretsTest {

    private static final Set<String> SECRET_FIELDS =
            Set.of("expectedResult", "solution", "testQueries", "correct", "requiredEvidence");

    @DynamicPropertySource
    static void databases(DynamicPropertyRegistry registry) {
        TestDatabases.register(registry);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    CaseRegistry cases;

    @Autowired
    ObjectMapper json;

    private List<MockHttpServletRequestBuilder> endpoints(LoadedCase c) {
        return List.of(
                get("/api/cases"),
                get("/api/cases/" + c.id()),
                post("/api/cases/" + c.id() + "/start").with(csrf()),
                get("/api/cases/" + c.id() + "/tables"),
                get("/api/cases/" + c.id() + "/tables/" + c.tables().get(0).name()),
                get("/api/cases/" + c.id() + "/evidence"),
                // ein frischer Spieler versucht, einen Beweis zu sichern - 409, ohne Text
                post("/api/cases/" + c.id() + "/evidence/" + c.definition().evidence().get(0).id()).with(csrf())
        );
    }

    @Test
    @DisplayName("Kein Fall-Endpunkt liefert Loesungsfelder, Referenzloesungen oder offene Beweise aus")
    void noEndpointLeaksSecrets() throws Exception {
        assertThat(cases.all()).isNotEmpty();
        for (LoadedCase c : cases.all()) {
            MockHttpSession freshPlayer = TestUsers.loggedIn(mvc);
            for (MockHttpServletRequestBuilder endpoint : endpoints(c)) {
                String body = mvc.perform(endpoint.session(freshPlayer)).andReturn().getResponse()
                        .getContentAsString();

                assertThat(fieldNames(json.readTree(body))).as("Felder in " + body).doesNotContainAnyElementsOf(SECRET_FIELDS);
                for (String secret : secretTexts(c)) {
                    assertThat(body).as("Antwort fuer " + c.id()).doesNotContain(secret);
                }
            }
        }
    }

    private static List<String> secretTexts(LoadedCase c) {
        List<String> secrets = new ArrayList<>();
        for (Mission m : c.definition().missions()) {
            secrets.add(m.solution());
            if (m.testQueries() != null) {
                secrets.addAll(m.testQueries().correct());
                secrets.addAll(m.testQueries().wrong());
            }
        }
        // ein frischer Spieler hat noch keinen Beweis gefunden
        c.definition().evidence().forEach(e -> secrets.add(e.text()));
        return secrets;
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new java.util.HashSet<>();
        collect(node, names);
        return names;
    }

    private static void collect(JsonNode node, Set<String> names) {
        if (node.isObject()) {
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String name = it.next();
                names.add(name);
                collect(node.get(name), names);
            }
        } else if (node.isArray()) {
            node.forEach(child -> collect(child, names));
        }
    }
}
