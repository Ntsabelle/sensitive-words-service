package com.joseph.sensitivewordsservice.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "A sensitive word as stored by the service")
public class SensitiveWordResponse {
    @Schema( example = "1")
    private Long id;
    @Schema(example = "create")
    private String word;
    @Schema(example = "true")
    private boolean active;
    @Schema(example = "2024-09-09T12:00:00Z")
    private Instant createdAt;
    @Schema(example = "2024-09-09T12:00:00Z")
    private Instant updatedAt;
}
