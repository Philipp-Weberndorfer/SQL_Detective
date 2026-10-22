package at.htlwels.sqldetective.auth;

/** Benutzername oder E-Mail-Adresse ist bereits vergeben. */
public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException(String message) {
        super(message);
    }
}
