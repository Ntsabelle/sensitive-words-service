package com.joseph.sensitivewordsservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to exchange a refresh token for a new access token, or to revoke
 * one on logout.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Refresh token exchange/revocation request",
    example = """
    {
      "refreshToken": "******"
    }"""
)
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    @Schema(description = "Refresh token previously issued at login", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
