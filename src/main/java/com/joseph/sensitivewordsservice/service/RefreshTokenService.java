package com.joseph.sensitivewordsservice.service;

import com.joseph.sensitivewordsservice.entity.RefreshToken;
import com.joseph.sensitivewordsservice.entity.User;
import com.joseph.sensitivewordsservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

/**
 * Issues, validates, and rotates refresh tokens.
 * <p>
 * Refresh tokens are opaque, cryptographically random values (not JWTs) so
 * that, unlike the stateless access token, they can be individually revoked
 * server-side before their natural expiry. Only a SHA-256 hash of each token
 * is persisted; the raw value is returned to the client exactly once, at
 * issuance/rotation time.
 * <p>
 * Rotation: every successful refresh revokes the presented token and issues
 * a brand new one. If an already-revoked (previously rotated or
 * explicitly-logged-out) token is ever presented again, every active
 * refresh token for that user is revoked, since reuse of a dead token is a
 * strong signal that the token was stolen.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshExpirationMs;

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    /**
     * Issue a brand new refresh token for the given user, persisting only its hash.
     *
     * @return the raw (unhashed) refresh token to return to the client
     */
    @Transactional
    public String issueToken(User user) {
        String rawToken = generateRawToken();

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(Instant.now().plusMillis(refreshExpirationMs))
                .revoked(false)
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validate a presented refresh token and rotate it: the old token is
     * revoked and a new one is issued in the same transaction.
     *
     * @param rawToken refresh token presented by the client
     * @return the authenticated user and the newly issued raw refresh token
     * @throws IllegalArgumentException if the token is unknown, expired, or
     *                                  already revoked (including reuse of a previously rotated token)
     */
    @Transactional
    public RotationResult validateAndRotate(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (Boolean.TRUE.equals(existing.getRevoked())) {
            log.warn("Reused or revoked refresh token presented for user: {} - revoking all sessions",
                    existing.getUser().getUsername());
            refreshTokenRepository.revokeAllForUser(existing.getUser().getId());
            throw new IllegalArgumentException("Refresh token has already been used");
        }

        if (existing.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token has expired");
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        String newRawToken = issueToken(existing.getUser());
        return new RotationResult(existing.getUser(), newRawToken);
    }

    /** Revoke a single refresh token, e.g. on logout. No-op if the token is unknown. */
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RotationResult(User user, String rawRefreshToken) {
    }
}
