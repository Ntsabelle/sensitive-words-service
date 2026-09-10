package com.joseph.sensitivewordsservice.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response containing the sanitized text")
public class SanitizeResponse {

    @Schema(description = "The original text", example = "This is a sample text with some sensitive words.")
    private String originalText;

    @Schema(description = "The number of sensitive words found and sanitized", example = "3")
    private int matchCount;

    @Schema(description = "The sanitized text", example = "This is a sample text with some **** words.")
    private String sanitizedText;
}
