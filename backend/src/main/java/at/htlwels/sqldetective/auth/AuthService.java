package at.htlwels.sqldetective.auth;

import at.htlwels.sqldetective.auth.AuthDtos.RegisterRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Registrierung und Laden von Benutzern fuer Spring Security (F01, F02).
 *
 * Passwoerter werden ausschliesslich als BCrypt-Hash gespeichert.
 */
@Service
public class AuthService implements UserDetailsService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (users.existsByUsername(username)) {
            throw new DuplicateUserException("Der Benutzername ist bereits vergeben.");
        }
        if (users.existsByEmail(email)) {
            throw new DuplicateUserException("Diese E-Mail-Adresse ist bereits registriert.");
        }

        try {
            return users.insert(username, email, passwordEncoder.encode(request.password()));
        } catch (DuplicateKeyException e) {
            // zwei gleichzeitige Registrierungen mit demselben Namen
            throw new DuplicateUserException("Benutzername oder E-Mail-Adresse ist bereits vergeben.");
        }
    }

    public AppUser getByUsername(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = getByUsername(username);
        return User.withUsername(user.username())
                .password(user.passwordHash())
                .roles(user.role().toUpperCase(Locale.ROOT))
                .build();
    }
}
