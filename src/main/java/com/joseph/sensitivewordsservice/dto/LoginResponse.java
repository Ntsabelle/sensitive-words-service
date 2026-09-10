package com.joseph.sensitivewordsservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JWT authentication response returned after successful login.
 * Client must include token in Authorization header for subsequent requests.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "JWT authentication response after successful login",
    example = """
    {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "username": "john_doe",
      "expiresIn": 120000
    }"""
)
public class LoginResponse {
    
    @Schema(
        description = "JWT Bearer token. Include in Authorization header: 'Bearer <token>'",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTUxNjIzOTAyMn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"
    )
    private String token;
    
    @Schema(
        description = "Username of authenticated user",
        example = "john_doe"
    )
    private String username;
    
    @Schema(
        description = "Token expiration time in milliseconds from issue time. Default: 120000 (2 minutes for testing, 86400000 for production)",
        example = "120000"
    )
    private Long expiresIn;
}
