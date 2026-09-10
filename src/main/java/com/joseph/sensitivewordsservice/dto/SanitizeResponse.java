package com.joseph.sensitivewordsservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Response DTO for text sanitization.
 * Contains original text, sanitized text with replacements, and match count.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    description = "Response containing sanitized text with sensitive words masked",
    example = """
    {
      "originalText": "This is a badword text that needs sanitization",
      "sanitizedText": "This is a ******* text that needs sanitization",
      "matchCount": 1
    }"""
)
public class SanitizeResponse {

    @Schema(
        description = "The original unmodified text provided in the request",
        example = "This is a badword text that needs sanitization"
    )
    private String originalText;

    @Schema(
        description = "Total number of sensitive words found and replaced in the text",
        example = "1"
    )
    private int matchCount;

    @Schema(
        description = "The sanitized text with all sensitive words replaced with asterisks (*). Each matched word is replaced with asterisks matching its original length.",
        example = "This is a ******* text that needs sanitization"
    )
    private String sanitizedText;
}
