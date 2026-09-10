package com.joseph.sensitivewordsservice.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Generic paginated response wrapper.
 * Converts Spring's Page object to JSON-friendly format.
 * 
 * @param <T> Type of elements in the content list
 * 
 * Example:
 * {
 *   "content": [...],
 *   "pageNumber": 0,
 *   "pageSize": 20,
 *   "totalElements": 50,
 *   "totalPages": 3,
 *   "last": false
 * }
 */
@Builder
@Schema(description = "Paged Response")
@Getter
@Setter
public class PagedResponse<T> {

    /** List of items on current page. */
    private List<T> content;
    
    /** Zero-based current page number. */
    private int pageNumber;
    
    /** Number of items per page. */
    private int pageSize;
    
    /** Total number of items across all pages. */
    private long totalElements;
    
    /** Total number of pages available. */
    private int totalPages;
    
    /** Whether this is the last page. */
    private boolean last;

    /**
     * Factory method to convert Spring Page to PagedResponse.
     * 
     * @param page Spring Page object
     * @param <T> Element type
     * @return PagedResponse with pagination metadata
     */
    public static <T> PagedResponse<T> from(Page<T> page) {
        return  PagedResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}