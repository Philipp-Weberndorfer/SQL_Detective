package at.htlwels.sqldetective.cases;

import at.htlwels.sqldetective.ApiTestBase;
import at.htlwels.sqldetective.TestDatabases;
import at.htlwels.sqldetective.TestUsers;
import at.htlwels.sqldetective.cases.CaseModel.LoadedCase;
import at.htlwels.sqldetective.cases.CaseModel.Mission;
import at.htlwels.sqldetective.game.ResultMatcher;
import at.htlwels.sqldetective.query.QueryDtos.QueryResult;
import at.htlwels.sqldetective.query.QueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Loesbarkeitstests je Fall (SCRUM-96, F128, F129).
 *
 * Laeuft automatisch fuer JEDEN geladenen Fall - ein neuer Fall-Ordner wird
 * mitgetestet, ohne dass hier Code geaendert werden muss. Jeder Test traegt
 * Fall und Mission im Namen, damit ein kaputter Fall sofort auffindbar ist.
 *
 * Alle Abfragen laufen wie im Spiel: Validierung, game_readonly, READ ONLY.
 */
class CaseSolvabilityTest extends ApiTestBase {

    @Autowired
    CaseRegistry cases;

    @Autowired
    QueryService queries;

    @Autowired
    ObjectMapper json;

    @Test
    @DisplayName("Jeder Fall-Ordner wurde fehlerfrei geladen")
    void everyCaseFolderIsLoaded() throws IOException {
        List<String> folders;
        try (Stream<Path> list = Files.list(TestDatabases.CASES_DIR)) {
            folders = list.filter(Files::isDirectory).map(p -> p.getFileName().toString()).sorted().toList();
        }
        List<String> loaded = cases.all().stream().map(LoadedCase::id).toList();
        assertThat(loaded).as("Faelle, die nicht geladen werden konnten - Formatfehler stehen im Log")
                .containsExactlyElementsOf(folders);
    }

    @TestFactory
    Stream<DynamicNode> everyCaseIsSolvable() {
        return cases.all().stream().map(c -> DynamicContainer.dynamicContainer(c.id(), Stream.concat(
                c.definition().missions().stream().map(m -> missionTests(c, m)),
                Stream.of(DynamicTest.dynamicTest(c.id() + ": Durchspielen bis zum Abschluss", () -> playThrough(c))))));
    }

    private DynamicContainer missionTests(LoadedCase c, Mission m) {
        String where = c.id() + " / " + m.id();
        List<DynamicTest> tests = new ArrayList<>();

        tests.add(DynamicTest.dynamicTest(where + ": Referenzloesung liefert genau das erwartete Ergebnis", () -> {
            QueryResult r = queries.execute(c.schema(), m.solution());
            assertEquals(m.expectedResult().columns().size(), r.columns().size(),
                    where + ": Referenzloesung liefert andere Spalten " + r.columns());
            assertTrue(matches(m, r), where + ": Referenzloesung liefert " + r.rows());
        }));

        tests.add(DynamicTest.dynamicTest(where + ": mindestens zwei Loesungswege und eine falsche Abfrage", () -> {
            assertThat(m.testQueries()).as(where + ": testQueries fehlt").isNotNull();
            assertThat(1 + m.testQueries().correct().size()).as(where + ": zu wenige richtige Loesungswege")
                    .isGreaterThanOrEqualTo(2);
            assertThat(m.testQueries().wrong()).as(where + ": keine falsche Abfrage").isNotEmpty();
        }));

        if (m.testQueries() != null) {
            for (String sql : m.testQueries().correct()) {
                tests.add(DynamicTest.dynamicTest(where + ": weiterer Loesungsweg wird erkannt", () ->
                        assertTrue(matches(m, queries.execute(c.schema(), sql)), where + ": nicht erkannt: " + sql)));
            }
            for (String sql : m.testQueries().wrong()) {
                tests.add(DynamicTest.dynamicTest(where + ": falsche Abfrage wird abgewiesen", () ->
                        assertFalse(matches(m, queries.execute(c.schema(), sql)), where + ": faelschlich richtig: " + sql)));
            }
        }
        return DynamicContainer.dynamicContainer(where, tests);
    }

    private static boolean matches(Mission m, QueryResult r) {
        return ResultMatcher.matches(m.expectedResult(), r.columns(), r.rows(), r.truncated());
    }

    /**
     * Wie ein Spieler: alle Missionen der Reihe nach loesen, jeden Beweis sichern,
     * dann die richtige Schlussfolgerung mit den geforderten Beweisen vorlegen.
     */
    private void playThrough(LoadedCase c) throws Exception {
        MockHttpSession player = TestUsers.loggedIn(mvc);

        for (Mission m : c.definition().missions()) {
            String where = c.id() + " / " + m.id();
            String body = json.writeValueAsString(Map.of("caseId", c.id(), "sql", m.solution()));
            String response = mvc.perform(post("/api/queries").with(csrf()).session(player)
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            var outcome = json.readTree(response).path("mission");
            assertEquals(m.id(), outcome.path("missionId").asText(), where + ": falsche Mission offen");
            assertTrue(outcome.path("solved").asBoolean(), where + ": Mission nicht geloest");

            if (m.evidenceOnSuccess() != null) {
                mvc.perform(post("/api/cases/" + c.id() + "/evidence/" + m.evidenceOnSuccess()).with(csrf()).session(player))
                        .andExpect(status().isOk());
            }
        }

        var conclusion = c.definition().conclusion();
        for (String required : conclusion.requiredEvidence()) {
            assertTrue(c.definition().missionAwarding(required).isPresent(),
                    c.id() + ": geforderter Beweis " + required + " wird von keiner Mission freigeschaltet");
        }
        String body = json.writeValueAsString(Map.of("answer", conclusion.correct(), "evidenceIds", conclusion.requiredEvidence()));
        String response = mvc.perform(post("/api/cases/" + c.id() + "/conclusion").with(csrf()).session(player)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(json.readTree(response).path("solved").asBoolean(), c.id() + ": richtige Schlussfolgerung abgelehnt");

        String status = json.readTree(mvc.perform(get("/api/cases/" + c.id()).session(player))
                .andReturn().getResponse().getContentAsString()).path("status").asText();
        assertEquals("SOLVED", status, c.id() + ": Fall nach dem Abschluss nicht geloest");
    }
}
