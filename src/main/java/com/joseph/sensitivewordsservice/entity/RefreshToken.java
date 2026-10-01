package com.joseph.sensitivewordsservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Refresh token entity - a long-lived, single-use token a client exchanges
 * for a new short-lived JWT access token without re-submitting credentials.
 * <p>
 * Unlike the stateless JWT access token, refresh tokens are opaque random
 * values tracked server-side so they can be individually revoked (logout,
 * suspected theft) before their natural expiry. Only a SHA-256 hash of the
 * raw token is persisted; the raw value is handed to the client exactly
 * once, at issuance/rotation time, and is never stored or logged.
 */
@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256 hash (base64) of the raw refresh token. The raw value is never stored. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** True once this token has been rotated/used or explicitly revoked (logout). */
    @Column(nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
