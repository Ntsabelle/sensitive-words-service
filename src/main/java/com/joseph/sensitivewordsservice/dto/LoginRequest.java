package com.joseph.sensitivewordsservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for user login or registration.
 * Used by both /auth/login and /auth/register endpoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "User authentication credentials for login or registration",
    example = """
    {
      "username": "john_doe",
      "password": "SecurePassword123!"
    }"""
)
public class LoginRequest {
    
    @NotBlank(message = "Username is required")
    @Schema(
        description = "Unique username for authentication. Case-sensitive.",
        example = "john_doe",
        minLength = 1,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(
        description = "Plain text password. Will be BCrypt hashed before storage. Never stored or returned in plain text.",
        example = "SecurePassword123!",
        minLength = 1,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String password;
}
