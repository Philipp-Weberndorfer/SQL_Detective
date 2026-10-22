package at.htlwels.sqldetective.auth;

import at.htlwels.sqldetective.auth.AuthDtos.LoginRequest;
import at.htlwels.sqldetective.auth.AuthDtos.RegisterRequest;
import at.htlwels.sqldetective.auth.AuthDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registrierung, Login und aktueller Benutzer (F01, F02).
 *
 * Der Login erzeugt eine Sitzung (Cookie JSESSIONID). Logout laeuft ueber
 * Spring Security selbst: POST /api/auth/logout, siehe SecurityConfig.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy contextHolder = SecurityContextHolder.getContextHolderStrategy();

    public AuthController(AuthService authService,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Neues Konto anlegen")
    @ApiResponse(responseCode = "201", description = "Konto angelegt - danach separat anmelden")
    @ApiResponse(responseCode = "400", description = "Feld leer oder ungueltig")
    @ApiResponse(responseCode = "409", description = "Benutzername oder E-Mail-Adresse vergeben")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.of(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Anmelden und Sitzung erzeugen")
    @ApiResponse(responseCode = "200", description = "Angemeldet, Sitzungs-Cookie gesetzt")
    @ApiResponse(responseCode = "401", description = "Benutzername oder Passwort falsch")
    public UserResponse login(@Valid @RequestBody LoginRequest request,
                              HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) {

        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));

        // Schutz vor Session Fixation: eine vorher bestehende Sitzung bekommt eine neue ID.
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }

        SecurityContext context = contextHolder.createEmptyContext();
        context.setAuthentication(auth);
        contextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        return UserResponse.of(authService.getByUsername(auth.getName()));
    }

    @GetMapping("/me")
    @Operation(summary = "Aktuell angemeldeten Benutzer abfragen")
    @ApiResponse(responseCode = "401", description = "Nicht angemeldet")
    public UserResponse me(Authentication authentication) {
        return UserResponse.of(authService.getByUsername(authentication.getName()));
    }
}
