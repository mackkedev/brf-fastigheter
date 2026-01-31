package se.fastighet.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import se.fastighet.core.dto.request.LoginRequest;
import se.fastighet.core.dto.request.TokenExchangeRequest;
import se.fastighet.core.dto.response.AuthResponse;
import se.fastighet.core.entity.User;
import se.fastighet.core.exception.UnauthorizedException;
import se.fastighet.core.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final FirebaseTokenValidator firebaseTokenValidator;

    /**
     * Login with email and password
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Felaktig e-post eller lösenord"));

        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new UnauthorizedException("Felaktig e-post eller lösenord");
        }

        String token = jwtService.generateToken(user);

        log.debug("User {} logged in successfully via email/password", user.getEmail());

        return buildAuthResponse(user, token);
    }

    /**
     * Exchange Firebase token for internal JWT token.
     * Validates the Firebase token, extracts user info, and issues an internal JWT.
     */
    public AuthResponse exchangeToken(TokenExchangeRequest request) {
        log.debug("Processing Firebase token exchange request");

        // Validate Firebase token
        FirebaseTokenValidator.FirebaseTokenResult tokenResult =
                firebaseTokenValidator.validateToken(request.getFirebaseToken());

        String email = tokenResult.email();
        String name = tokenResult.name();

        // Find or create user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .name(name != null ? name : email)
                            .role(User.Role.RESIDENT)
                            .build();
                    log.info("Creating new user from Firebase token: {}", email);
                    return userRepository.save(newUser);
                });

        // Generate internal JWT
        String token = jwtService.generateToken(user);

        log.info("Successfully exchanged Firebase token for user: {}", user.getEmail());

        return buildAuthResponse(user, token);
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationInSeconds())
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole())
                        .build())
                .build();
    }
}
