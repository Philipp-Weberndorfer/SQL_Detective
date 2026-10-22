package at.htlwels.sqldetective.auth;

import at.htlwels.sqldetective.TestDatabases;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests zu SCRUM-26: Registrierung, Login, Logout (F01, F02).
 * Je Fehlerfall aus den Akzeptanzkriterien mindestens ein Test.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    private static final String PASSWORD = "geheim123";

    @DynamicPropertySource
    static void databases(DynamicPropertyRegistry registry) {
        TestDatabases.register(registry);
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    @Qualifier("appJdbcTemplate")
    JdbcTemplate appDb;

    /** Jeder Test bekommt eigene Benutzer, damit sich Tests nicht stoeren. */
    private static String uniqueName() {
        return "u" + UUID.randomUUID().toString().substring(0, 8);
    }

    private ResultActions register(String username, String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"%s\", \"email\": \"%s\", \"password\": \"%s\"}"
                        .formatted(username, email, password)));
    }

    private ResultActions login(String username, String password, MockHttpSession session) throws Exception {
        return mvc.perform(post("/api/auth/login").with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\": \"%s\", \"password\": \"%s\"}".formatted(username, password)));
    }

    // ---------------------------------------------------------- Registrierung

    @Test
    @DisplayName("Registrierung legt ein Konto an")
    void registerCreatesAccount() throws Exception {
        String name = uniqueName();
        register(name, name + "@example.com", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.email").value(name + "@example.com"))
                .andExpect(jsonPath("$.role").value("player"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Passwoerter werden nur als BCrypt-Hash gespeichert")
    void passwordIsStoredHashed() throws Exception {
        String name = uniqueName();
        register(name, name + "@example.com", PASSWORD).andExpect(status().isCreated());

        String hash = appDb.queryForObject("SELECT password_hash FROM users WHERE username = ?", String.class, name);
        assertNotEquals(PASSWORD, hash);
        assertTrue(hash.startsWith("$2"), "BCrypt-Hash erwartet");
    }

    @Test
    @DisplayName("Fehlerfall: Benutzername vergeben (auch in anderer Schreibweise)")
    void usernameTaken() throws Exception {
        String name = uniqueName();
        register(name, name + "@example.com", PASSWORD).andExpect(status().isCreated());

        register(name.toUpperCase(), uniqueName() + "@example.com", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Der Benutzername ist bereits vergeben."));
    }

    @Test
    @DisplayName("Fehlerfall: E-Mail-Adresse vergeben")
    void emailTaken() throws Exception {
        String name = uniqueName();
        register(name, name + "@example.com", PASSWORD).andExpect(status().isCreated());

        register(uniqueName(), name + "@example.com", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Diese E-Mail-Adresse ist bereits registriert."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"username", "email", "password"})
    @DisplayName("Fehlerfall: leeres Feld bei der Registrierung")
    void registerWithEmptyField(String emptyField) throws Exception {
        String name = uniqueName();
        register(emptyField.equals("username") ? "" : name,
                emptyField.equals("email") ? "" : name + "@example.com",
                emptyField.equals("password") ? "" : PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("VALIDATION"));
    }

    @Test
    @DisplayName("Fehlerfall: ungueltige E-Mail-Adresse und zu kurzes Passwort")
    void registerWithInvalidValues() throws Exception {
        register(uniqueName(), "keine-adresse", PASSWORD).andExpect(status().isBadRequest());
        String name = uniqueName();
        register(name, name + "@example.com", "kurz").andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ Login

    @Test
    @DisplayName("Login erzeugt eine Sitzung, Logout beendet sie")
    void loginAndLogout() throws Exception {
        String name = uniqueName();
        register(name, name + "@example.com", PASSWORD).andExpect(status().isCreated());

        MockHttpSession session = new MockHttpSession();
        login(name, PASSWORD, session)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name));

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name));

        mvc.perform(post("/api/auth/logout").with(csrf()).session(session))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Mit Sitzung ist die Abfrage-API erreichbar")
    void sessionGrantsAccessToQueries() throws Exception {
        String name = uniqueName();
        register(name, name + "@example.com", PASSWORD).andExpect(status().isCreated());
        MockHttpSession session = new MockHttpSession();
        login(name, PASSWORD, session).andExpect(status().isOk());

        mvc.perform(post("/api/queries").with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sql\": \"SELECT 1 AS eins\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns[0]").value("eins"));
    }

    @Test
    @DisplayName("Fehlerfall: falsches Passwort")
    void wrongPassword() throws Exception {
        String name = uniqueName();
        register(name, name + "@example.com", PASSWORD).andExpect(status().isCreated());

        login(name, "falsch123", new MockHttpSession())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Benutzername oder Passwort ist falsch."));
    }

    @Test
    @DisplayName("Fehlerfall: unbekannter Benutzer bekommt dieselbe Meldung wie falsches Passwort")
    void unknownUser() throws Exception {
        login(uniqueName(), PASSWORD, new MockHttpSession())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Benutzername oder Passwort ist falsch."));
    }

    @Test
    @DisplayName("Fehlerfall: leere Felder beim Login")
    void loginWithEmptyFields() throws Exception {
        login("", "", new MockHttpSession())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("VALIDATION"));
    }

    @Test
    @DisplayName("Ohne CSRF-Token wird ein Login abgewiesen")
    void loginWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"x\", \"password\": \"y\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Health und API-Dokumentation sind ohne Anmeldung erreichbar")
    void publicEndpoints() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
