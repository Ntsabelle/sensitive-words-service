package com.joseph.sensitivewordsservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Getter;

/**
 * Request DTO for text sanitization.
 * Accepts arbitrary text and returns sanitized version with sensitive words masked.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(
    description = "Request to sanitize text by masking sensitive words with asterisks",
    example = """
    {
      "text": "This is a badword text that needs sanitization"
    }"""
)
public class SanitizeRequest {

    @NotBlank(message = "Input text must not be blank")
    @Schema(
        description = "Raw text to be sanitized. Will be searched for sensitive words (case-insensitive) and matches replaced with asterisks.",
        example = "This is a badword text that needs sanitization",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String text;
}
