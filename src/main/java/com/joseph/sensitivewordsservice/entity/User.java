package com.joseph.sensitivewordsservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

/**
 * User entity - represents an authenticated user for JWT-based access.
 * 
 * Stored in users table with:
 * - Unique username constraint for login identification
 * - BCrypt-hashed password (never stored in plain text)
 * - active flag to enable/disable accounts without deleting
 * - createdAt and updatedAt timestamps (auto-managed by Hibernate)
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique username used for login. Case-sensitive. */
    @Column(unique = true, nullable = false)
    private String username;

    /** BCrypt-hashed password. Never stored or returned in plain text. */
    @Column(nullable = false)
    private String password;

    /** Whether this user account is active. Defaults to true on registration. */
    @Column(nullable = false)
    private Boolean active = true;

    /**
     * Timestamp when account was created. Set automatically on insert.
     * Uses Hibernate @CreationTimestamp annotation for automatic management.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Timestamp when account was last updated. Auto-updated on each persist/update.
     * Uses Hibernate @UpdateTimestamp annotation for automatic management.
     */
    @UpdateTimestamp
    private Instant updatedAt;
}
