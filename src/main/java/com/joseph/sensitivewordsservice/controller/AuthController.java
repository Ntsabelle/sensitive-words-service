package com.joseph.sensitivewordsservice.controller;

import com.joseph.sensitivewordsservice.annotation.RateLimit;
import com.joseph.sensitivewordsservice.dto.ApiErrorResponse;
import com.joseph.sensitivewordsservice.dto.LoginRequest;
import com.joseph.sensitivewordsservice.dto.LoginResponse;
import com.joseph.sensitivewordsservice.dto.RefreshRequest;
import com.joseph.sensitivewordsservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
     * Validates credentials and returns a short-lived JWT access token
     * plus a longer-lived refresh token.
     *
     * Access token lifetime is controlled by jwt.expiration (default 15
     * minutes); refresh token lifetime by jwt.refresh-expiration (default
     * 7 days). Use POST /refresh to obtain a new access token once the
     * access token expires, without re-submitting credentials.
     *
     * @param request LoginRequest with username and password
     * @return LoginResponse containing access token, refresh token, username, and expirations
     * @throws IllegalArgumentException if credentials invalid or user inactive
     */
    @PostMapping("/login")
    @RateLimit(bucketName = "login")
    @Operation(
        summary = "Authenticate user and obtain JWT token",
        description = "Validate username and password credentials. Returns JWT bearer token for subsequent authenticated requests.",
        tags = {"Authentication"}
    )
    @ApiResponse(
        responseCode = "200",
        description = "Login successful - JWT token returned",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = LoginResponse.class),
            examples = @ExampleObject(value = """
            {
              "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTUxNjIzOTAyMn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U",
              "username": "john_doe",
              "expiresIn": 120000
            }""")
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Bad Request - Invalid username or password, or user account is inactive",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class),
            examples = @ExampleObject(value = """
            {
              "timestamp": "2024-09-10T21:48:24.441637700Z",
              "status": 400,
              "error": "Bad Request",
              "message": "Invalid username or password",
              "path": "/api/v1/auth/login"
            }""")
        )
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal Server Error - Unexpected error during authentication",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
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
     * New users are active by default.
     * 
     * @param request LoginRequest with username and password
     * @return 201 Created with success message
     * @throws IllegalArgumentException if username already exists
     */
    @PostMapping("/register")
    @RateLimit(bucketName = "login")
    @Operation(
        summary = "Register a new user account",
        description = "Create new user with provided username and password. Password is BCrypt hashed. Username must be globally unique.",
        tags = {"Authentication"}
    )
    @ApiResponse(
        responseCode = "201",
        description = "User registered successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(example = "\"User registered successfully\"")
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Bad Request - Username already exists or invalid input",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class),
            examples = @ExampleObject(value = """
            {
              "timestamp": "2024-09-10T21:33:22.822549100Z",
              "status": 400,
              "error": "Bad Request",
              "message": "Username already exists",
              "path": "/api/v1/auth/register"
            }""")
        )
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal Server Error - Unexpected error during registration",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    public ResponseEntity<String> register(@Valid @RequestBody LoginRequest request) {
        authService.registerUser(request.getUsername(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    /**
     * Exchange a refresh token for a new access token.
     * The presented refresh token is single-use: it is revoked and replaced
     * by a new one in the response. Reuse of an already-used refresh token
     * revokes every active session for that user, since it is a strong
     * signal the token was stolen.
     *
     * @param request RefreshRequest containing the refresh token
     * @return LoginResponse containing a new JWT access token and a new refresh token
     * @throws IllegalArgumentException if the refresh token is invalid, expired, or already used
     */
    @PostMapping("/refresh")
    @RateLimit(bucketName = "login")
    @Operation(
        summary = "Exchange a refresh token for a new access token",
        description = "Validates the presented refresh token and, if valid, rotates it: the old refresh " +
            "token is revoked and a new access token plus new refresh token are returned. Avoids requiring " +
            "the client to resend credentials once the short-lived access token expires.",
        tags = {"Authentication"}
    )
    @ApiResponse(
        responseCode = "200",
        description = "Refresh successful - new access token and refresh token returned",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = LoginResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Bad Request - refresh token missing, invalid, expired, or already used",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        LoginResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Log out by revoking the presented refresh token.
     * The current access token remains valid until its natural (short)
     * expiry, since JWT access tokens are stateless and not tracked
     * server-side; only the refresh token is revoked here.
     *
     * @param request RefreshRequest containing the refresh token to revoke
     * @return 204 No Content
     */
    @PostMapping("/logout")
    @Operation(
        summary = "Revoke a refresh token (logout)",
        description = "Revokes the given refresh token so it can no longer be exchanged for new access " +
            "tokens. Idempotent and does not reveal whether the token was valid.",
        tags = {"Authentication"}
    )
    @ApiResponse(responseCode = "204", description = "Refresh token revoked (or was already invalid)")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
