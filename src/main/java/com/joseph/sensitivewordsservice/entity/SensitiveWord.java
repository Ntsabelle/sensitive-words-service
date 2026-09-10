package com.joseph.sensitivewordsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * SensitiveWord entity - represents a word that should be sanitized in text.
 * 
 * Stored in sensitive_words table with:
 * - Unique constraint on word (case-insensitive, enforced at service level)
 * - Index on word column for fast sanitization lookups
 * - createdAt and updatedAt timestamps (auto-managed)
 * - active flag to enable/disable without deleting
 */
@Entity
@Table(name = "sensitive_words",
        uniqueConstraints = @UniqueConstraint(name = "uk_sensitive_words_word",columnNames = "word"),
        indexes = @Index(name = "idx_sensitive_words_active_word", columnList = "word")
)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SensitiveWord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The sensitive word to match and sanitize. Must be unique. */
    @Column(name = "word", nullable = false, unique = true)
    private String word;

    /** Whether this word should be actively sanitized. Defaults to true. */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    /** Timestamp when word was created. Set automatically on insert. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Timestamp when word was last updated. Updated on insert and update. */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Auto-set createdAt and updatedAt on initial persist. */
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    /** Auto-update updatedAt on each update. */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
