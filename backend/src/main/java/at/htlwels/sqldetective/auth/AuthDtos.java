package at.htlwels.sqldetective.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datenobjekte fuer Registrierung und Login (F01, F02).
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Bitte gib einen Benutzernamen an.")
            @Size(min = 3, max = 50, message = "Der Benutzername muss 3 bis 50 Zeichen lang sein.")
            @Pattern(regexp = "^[A-Za-z0-9_.-]*$",
                    message = "Der Benutzername darf nur Buchstaben, Ziffern sowie _ . - enthalten.")
            String username,

            @NotBlank(message = "Bitte gib eine E-Mail-Adresse an.")
            @Email(message = "Bitte gib eine gueltige E-Mail-Adresse an.")
            @Size(max = 255, message = "Die E-Mail-Adresse ist zu lang.")
            String email,

            // BCrypt beruecksichtigt nur die ersten 72 Byte - laengere Passwoerter abweisen.
            @NotBlank(message = "Bitte gib ein Passwort an.")
            @Size(min = 8, max = 72, message = "Das Passwort muss 8 bis 72 Zeichen lang sein.")
            String password
    ) {
    }

    public record LoginRequest(
            @NotBlank(message = "Bitte gib deinen Benutzernamen an.")
            String username,

            @NotBlank(message = "Bitte gib dein Passwort an.")
            String password
    ) {
    }

    /** Was das Frontend ueber den angemeldeten Benutzer erfaehrt - niemals den Hash. */
    public record UserResponse(long id, String username, String email, String role) {

        static UserResponse of(AppUser user) {
            return new UserResponse(user.id(), user.username(), user.email(), user.role());
        }
    }
}
