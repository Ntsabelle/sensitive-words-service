package com.joseph.sensitivewordsservice.controller;


import com.joseph.sensitivewordsservice.annotation.RateLimit;
import com.joseph.sensitivewordsservice.dto.*;
import com.joseph.sensitivewordsservice.entity.SensitiveWord;
import com.joseph.sensitivewordsservice.mapper.SensitiveWordMapper;
import com.joseph.sensitivewordsservice.repository.SensitiveWordRepository;
import com.joseph.sensitivewordsservice.service.SensitiveWordService;
import com.joseph.sensitivewordsservice.service.SanitizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sensitive-words")
@RequiredArgsConstructor
@Tag(name = "Sensitive Words", description = "Manage sensitive words used for text sanitization")
@SecurityRequirement(name = "bearer-jwt")
public class SensitiveWordController {
    private final SensitiveWordService sensitiveWordService;
    private final SanitizationService sanitizationService;

    @PostMapping
    @RateLimit(bucketName = "sensitiveWord")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create a new sensitive word",
        description = "Add a new word to the sensitive words database. Word will be immediately available for sanitization. Word must be unique (case-insensitive)."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Word created successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = SensitiveWordResponse.class),
            examples = @ExampleObject(value = """
            {
              "id": 1,
              "word": "create",
              "active": true,
              "createdAt": "2024-09-09T12:00:00Z",
              "updatedAt": "2024-09-09T12:00:00Z"
            }""")
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Bad Request - Word already exists or invalid input",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal Server Error",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    public SensitiveWordResponse create(@Valid @RequestBody SensitiveWordRequest request) {
        SensitiveWord created = sensitiveWordService.create(request);
        // Refresh cache immediately so sanitization uses the new word
        sanitizationService.refresh();
        return SensitiveWordMapper.toResponse(created);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Retrieve a sensitive word by ID",
        description = "Get details of a specific sensitive word including its status, creation date, and update date."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Word found and returned successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = SensitiveWordResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Not Found - Word with specified ID does not exist",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class),
            examples = @ExampleObject(value = """
            {
              "timestamp": "2024-09-10T21:48:24.441637700Z",
              "status": 404,
              "error": "Not Found",
              "message": "Sensitive word not found with id: 999",
              "path": "/api/v1/sensitive-words/999"
            }""")
        )
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal Server Error",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    public SensitiveWordResponse getById(
        @PathVariable 
        @Parameter(description = "Unique identifier of the sensitive word", example = "1")
        Long id
    ) {
        return SensitiveWordMapper.toResponse(sensitiveWordService.getById(id));
    }

    @GetMapping
    @Operation(
        summary = "List sensitive words with pagination",
        description = "Retrieve paginated list of sensitive words. Supports filtering by active status and custom pagination settings."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Words retrieved successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = PagedResponse.class),
            examples = @ExampleObject(value = """
            {
              "content": [
                {
                  "id": 1,
                  "word": "create",
                  "active": true,
                  "createdAt": "2024-09-09T12:00:00Z",
                  "updatedAt": "2024-09-09T12:00:00Z"
                }
              ],
              "pageNumber": 0,
              "pageSize": 20,
              "totalElements": 1,
              "totalPages": 1,
              "last": true
            }""")
        )
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal Server Error",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    public PagedResponse<SensitiveWordResponse> list(
            @RequestParam(required = false)
            @Parameter(description = "Filter by active status. true = only active words, false = only inactive words, null = all words", example = "true")
            Boolean active,
            @PageableDefault(size = 20)
            @Parameter(description = "Pagination parameters: page (zero-based), size (max 100), sort (e.g., id,desc)")
            Pageable pageable) {
        Page<SensitiveWordResponse> page = sensitiveWordService.list(active, pageable)
                .map(SensitiveWordMapper::toResponse);
        return PagedResponse.from(page);
    }

    @PutMapping("/{id}")
    @RateLimit(bucketName = "sensitiveWord")
    @Operation(
        summary = "Update a sensitive word",
        description = "Update word text or active status. Word is immediately updated in sanitization rules."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Word updated successfully",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = SensitiveWordResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "Bad Request - New word already exists or invalid input",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Not Found - Word with specified ID does not exist",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal Server Error",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    public SensitiveWordResponse update(
        @PathVariable
        @Parameter(description = "Unique identifier of the word to update", example = "1")
        Long id,
        @Valid @RequestBody SensitiveWordRequest request
    ) {
        SensitiveWord updated = sensitiveWordService.update(id, request);
        // Refresh cache so sanitization uses updated word
        sanitizationService.refresh();
        return SensitiveWordMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    @RateLimit(bucketName = "sensitiveWord")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Delete a sensitive word",
        description = "Remove a word from the database. Word is immediately removed from sanitization rules. Returns 204 No Content on success."
    )
    @ApiResponse(
        responseCode = "204",
        description = "Word deleted successfully"
    )
    @ApiResponse(
        responseCode = "401",
        description = "Unauthorized - Missing or invalid JWT token",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "404",
        description = "Not Found - Word with specified ID does not exist",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    @ApiResponse(
        responseCode = "500",
        description = "Internal Server Error",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiErrorResponse.class)
        )
    )
    public void delete(
        @PathVariable
        @Parameter(description = "Unique identifier of the word to delete", example = "1")
        Long id
    ) {
        sensitiveWordService.delete(id);
        // Refresh cache so sanitization no longer uses deleted word
        sanitizationService.refresh();
    }
}
