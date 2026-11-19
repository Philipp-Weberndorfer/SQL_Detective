package at.htlwels.sqldetective;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Echte Benutzer fuer API-Tests: registrieren und anmelden ueber die API.
 * Jeder Aufruf bekommt einen eigenen Namen, damit sich Tests nicht stoeren.
 */
public final class TestUsers {

    public static final String PASSWORD = "geheim123";

    private TestUsers() {
    }

    public static String uniqueName() {
        return "u" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Neuer Benutzer, angemeldet - die Sitzung fuer weitere Aufrufe. */
    public static MockHttpSession loggedIn(MockMvc mvc) throws Exception {
        String name = uniqueName();
        register(mvc, name);
        return login(mvc, name);
    }

    public static void register(MockMvc mvc, String name) throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"%s\", \"email\": \"%s@example.com\", \"password\": \"%s\"}"
                                .formatted(name, name, PASSWORD)))
                .andExpect(status().isCreated());
    }

    public static MockHttpSession login(MockMvc mvc, String name) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"%s\", \"password\": \"%s\"}".formatted(name, PASSWORD)))
                .andExpect(status().isOk());
        return session;
    }
}
