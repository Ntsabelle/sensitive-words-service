package com.joseph.sensitivewordsservice.service;

import com.joseph.sensitivewordsservice.dto.LoginRequest;
import com.joseph.sensitivewordsservice.dto.LoginResponse;
import com.joseph.sensitivewordsservice.dto.RefreshRequest;
import com.joseph.sensitivewordsservice.entity.User;
import com.joseph.sensitivewordsservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * Authenticate user with username and password.
     * Validates credentials, checks if account is active,
     * and returns JWT token valid for configured expiration time.
     * 
     * @param request LoginRequest with username and password
     * @return LoginResponse with JWT token and expiration
     * @throws IllegalArgumentException if credentials invalid or user inactive
     */
    public LoginResponse login(LoginRequest request) {
        // Find user by username (uses cache if available)
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login attempt with invalid username: {}", request.getUsername());
                    return new IllegalArgumentException("Invalid username or password");
                });

        // Check if account is active
        if (!user.getActive()) {
            log.warn("Login attempt for inactive user: {}", request.getUsername());
            throw new IllegalArgumentException("User account is inactive");
        }

        // Validate password using BCrypt comparison
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login attempt with invalid password for user: {}", request.getUsername());
            throw new IllegalArgumentException("Invalid username or password");
        }

        // Generate JWT token with configured expiration
        String token = jwtService.generateToken(user.getUsername());
        String rawRefreshToken = refreshTokenService.issueToken(user);
        log.info("User logged in successfully: {}", user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .expiresIn(expiration)
                .refreshToken(rawRefreshToken)
                .refreshExpiresIn(refreshTokenService.getRefreshExpirationMs())
                .build();
    }

    /**
     * Exchange a refresh token for a new access token.
     * The presented refresh token is rotated: it is revoked and a new one
     * is issued alongside the new access token. Reuse of an already-used
     * refresh token revokes every active session for that user, since it
     * is a strong signal the token was stolen.
     *
     * @param request RefreshRequest containing the refresh token
     * @return LoginResponse with a new access token and a new rotated refresh token
     * @throws IllegalArgumentException if the refresh token is invalid, expired, or already used
     */
    public LoginResponse refresh(RefreshRequest request) {
        RefreshTokenService.RotationResult result = refreshTokenService.validateAndRotate(request.getRefreshToken());
        User user = result.user();

        String token = jwtService.generateToken(user.getUsername());
        log.info("Access token refreshed for user: {}", user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .expiresIn(expiration)
                .refreshToken(result.rawRefreshToken())
                .refreshExpiresIn(refreshTokenService.getRefreshExpirationMs())
                .build();
    }

    /**
     * Log out by revoking the presented refresh token. Idempotent: revoking
     * an already-revoked or unknown token is a no-op, so this never leaks
     * whether a given token was valid.
     *
     * @param request RefreshRequest containing the refresh token to revoke
     */
    public void logout(RefreshRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
        log.info("Refresh token revoked (logout)");
    }

    /**
     * Register a new user account.
     * Checks for duplicate username and hashes password using BCrypt.
     * Invalidates username cache on successful registration.
     * 
     * @param username Unique username for the account
     * @param password Plain text password (will be hashed)
     * @return Saved User entity
     * @throws IllegalArgumentException if username already exists
     */
    @CacheEvict(value = "users", key = "#username")
    public User registerUser(String username, String password) {
        // Check if username already exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Hash password using BCrypt (strength 10)
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("User registered successfully: {}", username);
        return saved;
    }
}
