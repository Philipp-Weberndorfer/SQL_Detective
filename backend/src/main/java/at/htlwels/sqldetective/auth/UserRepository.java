package at.htlwels.sqldetective.auth;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Zugriff auf die Tabelle users in der Anwendungsdatenbank.
 *
 * Benutzernamen und E-Mail-Adressen werden ohne Beachtung der Gross- und
 * Kleinschreibung verglichen - "Max" und "max" sind derselbe Benutzer.
 */
@Repository
public class UserRepository {

    private static final RowMapper<AppUser> MAPPER = (rs, i) -> new AppUser(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("role"));

    private final JdbcTemplate jdbc;

    public UserRepository(@Qualifier("appJdbcTemplate") JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AppUser> findByUsername(String username) {
        return jdbc.query("SELECT * FROM users WHERE LOWER(username) = LOWER(?)", MAPPER, username)
                .stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM users WHERE LOWER(username) = LOWER(?))", Boolean.class, username));
    }

    public boolean existsByEmail(String email) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM users WHERE LOWER(email) = LOWER(?))", Boolean.class, email));
    }

    public AppUser insert(String username, String email, String passwordHash) {
        return jdbc.queryForObject("""
                INSERT INTO users (username, email, password_hash, display_name)
                VALUES (?, ?, ?, ?)
                RETURNING *
                """, MAPPER, username, email, passwordHash, username);
    }
}
