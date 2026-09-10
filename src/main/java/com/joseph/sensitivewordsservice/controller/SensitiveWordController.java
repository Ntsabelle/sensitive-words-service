package com.joseph.sensitivewordsservice.controller;


import com.joseph.sensitivewordsservice.dto.PagedResponse;
import com.joseph.sensitivewordsservice.dto.SensitiveWordRequest;
import com.joseph.sensitivewordsservice.dto.SensitiveWordResponse;
import com.joseph.sensitivewordsservice.entity.SensitiveWord;
import com.joseph.sensitivewordsservice.mapper.SensitiveWordMapper;
import com.joseph.sensitivewordsservice.repository.SensitiveWordRepository;
import com.joseph.sensitivewordsservice.service.SensitiveWordService;
import com.joseph.sensitivewordsservice.service.SanitizationService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Sensitive Words", description = "Manage the list of sensitive masks out of incoming words")
public class SensitiveWordController {
    private final SensitiveWordService sensitiveWordService;
    private final SanitizationService sanitizationService;

    /**
     * Create a new sensitive word entry.
     * After creation, refreshes the sanitization cache so new words are immediately active.
     * 
     * @param request SensitiveWordRequest with word, category, severity
     * @return Created SensitiveWordResponse with assigned ID and timestamps
     * @throws IllegalArgumentException if word already exists
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new sensitive word")
    public SensitiveWordResponse create(@Valid @RequestBody SensitiveWordRequest request) {
        SensitiveWord created = sensitiveWordService.create(request);
        // Refresh cache immediately so sanitization uses the new word
        sanitizationService.refresh();
        return SensitiveWordMapper.toResponse(created);
    }

    /**
     * Retrieve a specific sensitive word by its ID.
     * 
     * @param id The sensitive word ID
     * @return SensitiveWordResponse with full details
     * @throws EntityNotFoundException if word not found
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a sensitive word by ID")
    public SensitiveWordResponse getById(@PathVariable Long id) {
        return SensitiveWordMapper.toResponse(sensitiveWordService.getById(id));
    }

    /**
     * List all sensitive words with pagination.
     * Optionally filter by active status.
     * 
     * @param active Filter by active status (optional)
     * @param pageable Page number and size (default: page 0, size 20)
     * @return PagedResponse containing list of words and pagination metadata
     */
    @GetMapping
    @Operation(summary = "List sensitive words")
    public PagedResponse<SensitiveWordResponse> list(
            @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SensitiveWordResponse> page = sensitiveWordService.list(active, pageable)
                .map(SensitiveWordMapper::toResponse);
        return PagedResponse.from(page);
    }

    /**
     * Update an existing sensitive word.
     * After update, refreshes the sanitization cache.
     * 
     * @param id The word ID to update
     * @param request Updated SensitiveWordRequest
     * @return Updated SensitiveWordResponse
     * @throws EntityNotFoundException if word not found
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a sensitive word by ID")
    public SensitiveWordResponse update(@PathVariable Long id, @Valid @RequestBody SensitiveWordRequest request) {
        SensitiveWord updated = sensitiveWordService.update(id, request);
        // Refresh cache so sanitization uses updated word
        sanitizationService.refresh();
        return SensitiveWordMapper.toResponse(updated);
    }

    /**
     * Delete a sensitive word by ID.
     * After deletion, refreshes the sanitization cache.
     * Returns NO_CONTENT (204) on success.
     * 
     * @param id The word ID to delete
     * @throws EntityNotFoundException if word not found
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a sensitive word by ID")
    public void delete(@PathVariable Long id) {
        sensitiveWordService.delete(id);
        // Refresh cache so sanitization no longer uses deleted word
        sanitizationService.refresh();
    }
}
