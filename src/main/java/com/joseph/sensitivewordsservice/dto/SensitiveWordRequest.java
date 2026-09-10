package com.joseph.sensitivewordsservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO for creating or updating a sensitive word.
 * Used by word management endpoints (POST, PUT).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Request to create or update a sensitive word in the database",
    example = """
    {
      "word": "sensitiveWord",
      "active": true
    }"""
)
public class SensitiveWordRequest {
    
    @NotBlank(message = "Input word must not be blank")
    @Size(min = 1, max = 255, message = "Input word must be between 1 and 255 characters long")
    @Schema(
        description = "The sensitive word to be masked during sanitization. Stored as-is (case-sensitive). Must be unique.",
        example = "sensitiveWord",
        minLength = 1,
        maxLength = 255,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String word;

    @Schema(
        description = "Whether this word should be actively used in sanitization. If false, word is stored but not used. Defaults to true.",
        example = "true",
        defaultValue = "true"
    )
    private Boolean active = true;
}
