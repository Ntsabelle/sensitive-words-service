package com.joseph.sensitivewordsservice.controller;



import com.joseph.sensitivewordsservice.dto.SanitizeRequest;
import com.joseph.sensitivewordsservice.dto.SanitizeResponse;
import com.joseph.sensitivewordsservice.service.SanitizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sanitize")
@RequiredArgsConstructor
@Tag(name = "Sanitize", description = "Mask sensitive words in out of arbitrary text")
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
     */
    @PostMapping
    @Operation(summary = "Sanitize string")
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
