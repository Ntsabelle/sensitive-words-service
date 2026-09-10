package com.joseph.sensitivewordsservice.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter

@NoArgsConstructor
@AllArgsConstructor

@Builder
@Schema(description = "API Error Response")
public class ApiErrorResponse {

    @Schema(example = "2026-09-10T12:34:56.789Z")
    private Instant timestamp;

    @Schema(example = "400")
    private int status;

    @Schema(example = "Bad Request")
    private String error;

    @Schema(example = "The input text must not be blank.")
    private String message;

    @Schema (example = "/api/v1/sanitize")
    private String path;

    @Schema(description = "List of detailed error messages", example = "[\"The input text must not be blank.\", \"The input text must be at least 10 characters long.\"]")
    private List<String> details;
}
