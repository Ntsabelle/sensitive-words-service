package com.joseph.sensitivewordsservice.repository;

import com.joseph.sensitivewordsservice.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository for RefreshToken entity persistence.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revoke every currently-active refresh token for a user.
     * Used when a rotated (already-used) token is replayed, which is a
     * strong signal of token theft - kill all of that user's sessions.
     *
     * @return number of tokens revoked
     */
    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.user.id = :userId and r.revoked = false")
    int revokeAllForUser(@Param("userId") Long userId);
}
