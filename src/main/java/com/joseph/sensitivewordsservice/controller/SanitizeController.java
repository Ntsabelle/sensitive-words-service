package com.joseph.sensitivewordsservice.controller;



import com.joseph.sensitivewordsservice.annotation.RateLimit;
import com.joseph.sensitivewordsservice.dto.ApiErrorResponse;
import com.joseph.sensitivewordsservice.dto.SanitizeRequest;
import com.joseph.sensitivewordsservice.dto.SanitizeResponse;
import com.joseph.sensitivewordsservice.service.SanitizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sanitize")
@RequiredArgsConstructor
@Tag(name = "Sanitize", description = "Mask sensitive words in arbitrary text")
public class SanitizeController {
    private final SanitizationService sanitizationService;

    /**
     * Sanitize input text by replacing sensitive words with asterisks.
     * 
     * Uses precompiled regex pattern for high performance.
     * Matching is case-insensitive and respects word boundaries.
     * 
     * @param request SanitizeRequest containing text to sanitize
     * @return SanitizeResponse with originalText, sanitizedText, and matchCount
     * 
     * Example:
     * Input:  "This is a badword text"
     * Output: "This is a ******* text" (matchCount: 1)
     * 
     * Authentication: Requires valid JWT token in Authorization header
     */
    @PostMapping
    @RateLimit(bucketName = "sanitize")
    @Operation(
        summary = "Sanitize text by masking sensitive words",
        description = "Replace all occurrences of sensitive words with asterisks (*). Matching is case-insensitive. Each word is replaced with asterisks matching its length.",
        tags = {"Sanitize"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Text to sanitize",
            required = true,
            content = @io.swagger.v3.oas.annotations.media.Content(
                mediaType = "application/json",
                schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SanitizeRequest.class)
            )
        )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Text sanitized successfully",
        content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = SanitizeResponse.class),
            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                value = """
                {
                  "originalText": "This is a badword text that needs sanitization",
                  "sanitizedText": "This is a ******* text that needs sanitization",
                  "matchCount": 1
                }"""
            )
        )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "400",
        description = "Bad Request - Invalid or empty text provided",
        content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class)
        )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token in Authorization header. Include 'Bearer <token>' in header.",
        content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class)
        )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "500",
        description = "Internal Server Error - Unexpected error during sanitization",
        content = @io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = ApiErrorResponse.class)
        )
    )
    @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearer-jwt")
    public SanitizeResponse sanitize (@Valid @RequestBody SanitizeRequest request) {
        // Call sanitization service - returns text with replacements and match count
        SanitizationService.SanitizeResult result = sanitizationService.sanitize(request.getText());

        // Build response with original text for reference
        return SanitizeResponse.builder()
                .originalText(request.getText())
                .sanitizedText(result.sanitizedText())
                .matchCount(result.matchCount())
                .build();
    }

}
