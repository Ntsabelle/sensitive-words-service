package com.joseph.sensitivewordsservice.controller;

import com.joseph.sensitivewordsservice.dto.LoginRequest;
import com.joseph.sensitivewordsservice.dto.LoginResponse;
import com.joseph.sensitivewordsservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User authentication and JWT token management")
public class AuthController {

    private final AuthService authService;

    /**
     * User login endpoint.
     * Validates credentials and returns JWT token if successful.
     * 
     * Token is valid for duration specified in jwt.expiration config.
     * Default: 2 minutes (120000ms)
     * 
     * @param request LoginRequest with username and password
     * @return LoginResponse containing JWT token, username, and expiration time
     * @throws IllegalArgumentException if credentials invalid or user inactive
     */
    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * User registration endpoint.
     * Creates new user account with hashed password.
     * 
     * Password is hashed using BCrypt (strength 10).
     * Username must be unique.
     * 
     * @param request LoginRequest with username and password
     * @return 201 Created with success message
     * @throws IllegalArgumentException if username already exists
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<String> register(@Valid @RequestBody LoginRequest request) {
        authService.registerUser(request.getUsername(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }
}
