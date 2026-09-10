package com.joseph.sensitivewordsservice.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

/**
 * Response DTO for sensitive word stored in database.
 * Returned by word management endpoints (GET, POST, PUT, DELETE).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
    description = "A sensitive word stored in the database",
    example = """
    {
      "id": 1,
      "word": "create",
      "active": true,
      "createdAt": "2024-09-09T12:00:00Z",
      "updatedAt": "2024-09-09T12:00:00Z"
    }"""
)
public class SensitiveWordResponse {
    
    @Schema(
        description = "Unique identifier for the word",
        example = "1"
    )
    private Long id;
    
    @Schema(
        description = "The sensitive word to be masked. Case-sensitive as stored.",
        example = "create"
    )
    private String word;
    
    @Schema(
        description = "Whether this word is actively used in sanitization. If false, word is ignored.",
        example = "true"
    )
    private boolean active;
    
    @Schema(
        description = "ISO 8601 timestamp when word was created",
        example = "2024-09-09T12:00:00Z"
    )
    private Instant createdAt;
    
    @Schema(
        description = "ISO 8601 timestamp when word was last updated",
        example = "2024-09-09T12:00:00Z"
    )
    private Instant updatedAt;
}
