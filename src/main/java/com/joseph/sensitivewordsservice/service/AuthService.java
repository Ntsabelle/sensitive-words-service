package com.joseph.sensitivewordsservice.service;

import com.joseph.sensitivewordsservice.dto.LoginRequest;
import com.joseph.sensitivewordsservice.dto.LoginResponse;
import com.joseph.sensitivewordsservice.entity.User;
import com.joseph.sensitivewordsservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

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
        // Find user by username
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
        log.info("User logged in successfully: {}", user.getUsername());

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .expiresIn(expiration)
                .build();
    }

    /**
     * Register a new user account.
     * Checks for duplicate username and hashes password using BCrypt.
     * 
     * @param username Unique username for the account
     * @param password Plain text password (will be hashed)
     * @return Saved User entity
     * @throws IllegalArgumentException if username already exists
     */
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
