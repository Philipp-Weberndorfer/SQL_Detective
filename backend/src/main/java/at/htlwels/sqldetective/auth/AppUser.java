package at.htlwels.sqldetective.auth;

/** Ein Eintrag aus der Tabelle users der Anwendungsdatenbank. */
public record AppUser(
        long id,
        String username,
        String email,
        String passwordHash,
        String role
) {
}
