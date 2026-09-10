package com.joseph.sensitivewordsservice.repository;

import com.joseph.sensitivewordsservice.entity.SensitiveWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import javax.print.DocFlavor;
import java.util.List;
import java.util.Optional;

/**
 * Repository for SensitiveWord entity persistence.
 * Provides database access for CRUD operations and queries needed by sanitization service.
 * 
 * Key methods:
 * - findAllActiveWords(): Used to build regex pattern for sanitization (cache refresh)
 * - existsByWordIgnoreCase(): Validate no duplicates on create/update
 * - findAllByActive(): Paginated list for API responses
 */
public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, Long> {

    /**
     * Find word by case-insensitive match.
     * 
     * @param word Word to search for
     * @return Optional containing word if found
     */
    Optional<SensitiveWord> findByWordIgnoreCase(String word);

    /**
     * Check if word exists (case-insensitive).
     * Used to validate uniqueness on create/update operations.
     * 
     * @param word Word to check
     * @return true if word exists, false otherwise
     */
    boolean existsByWordIgnoreCase(String word);

    /**
     * Get all active words as simple string list.
     * Used by SanitizationService to build regex pattern on cache refresh.
     * Only includes words where active = true.
     * 
     * @return List of active word strings (case-sensitive as stored)
     */
    @Query("SELECT s.word FROM SensitiveWord s WHERE s.active = true")
    List<String> findAllActiveWords();

    /**
     * Get all active words with full entity details.
     * 
     * @return List of active SensitiveWord entities
     */
    List<SensitiveWord> findAllByActiveTrue();

    /**
     * Get paginated list of words filtered by active status.
     * Used by SensitiveWordController for list endpoint.
     * 
     * @param active Filter by active status
     * @param pageable Pagination info (page, size, sort)
     * @return Page of SensitiveWord entities
     */
    Page<SensitiveWord> findAllByActive(boolean active, Pageable pageable);
}
