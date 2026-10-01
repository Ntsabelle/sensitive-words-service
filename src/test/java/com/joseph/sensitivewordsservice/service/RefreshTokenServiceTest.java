package com.joseph.sensitivewordsservice.service;

import com.joseph.sensitivewordsservice.entity.RefreshToken;
import com.joseph.sensitivewordsservice.entity.User;
import com.joseph.sensitivewordsservice.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository);
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);

        user = User.builder()
                .id(1L)
                .username("john_doe")
                .password("hashed")
                .active(true)
                .build();
    }

    @Test
    void issueToken_persistsHashedTokenAndReturnsRawValue() {
        String rawToken = refreshTokenService.issueToken(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken saved = captor.getValue();
        assertNotNull(rawToken);
        assertEquals(user, saved.getUser());
        assertFalse(saved.getRevoked());
        assertNotEquals(rawToken, saved.getTokenHash(), "raw token must never be persisted directly");
        assertTrue(saved.getExpiresAt().isAfter(Instant.now()));
    }

    @Test
    void validateAndRotate_withValidToken_revokesOldAndIssuesNewToken() {
        RefreshToken existing = RefreshToken.builder()
                .id(10L)
                .user(user)
                .tokenHash("irrelevant-hash")
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        RefreshTokenService.RotationResult result = refreshTokenService.validateAndRotate("raw-token-value");

        assertTrue(existing.getRevoked(), "presented token must be revoked after use");
        assertEquals(user, result.user());
        assertNotNull(result.rawRefreshToken());

        // one save() for revoking the old token, one for persisting the newly issued token
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        verify(refreshTokenRepository, never()).revokeAllForUser(anyLong());
    }

    @Test
    void validateAndRotate_withUnknownToken_throws() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.validateAndRotate("does-not-exist"));
    }

    @Test
    void validateAndRotate_withExpiredToken_throws() {
        RefreshToken expired = RefreshToken.builder()
                .id(11L)
                .user(user)
                .tokenHash("irrelevant-hash")
                .revoked(false)
                .expiresAt(Instant.now().minusSeconds(1))
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.validateAndRotate("expired-raw-token"));
    }

    @Test
    void validateAndRotate_withAlreadyRevokedToken_revokesAllSessionsAndThrows() {
        RefreshToken reused = RefreshToken.builder()
                .id(12L)
                .user(user)
                .tokenHash("irrelevant-hash")
                .revoked(true)
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(reused));

        assertThrows(IllegalArgumentException.class,
                () -> refreshTokenService.validateAndRotate("already-used-raw-token"));

        verify(refreshTokenRepository).revokeAllForUser(user.getId());
    }

    @Test
    void revoke_withKnownToken_marksItRevoked() {
        RefreshToken existing = RefreshToken.builder()
                .id(13L)
                .user(user)
                .tokenHash("irrelevant-hash")
                .revoked(false)
                .expiresAt(Instant.now().plusSeconds(60))
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        refreshTokenService.revoke("some-raw-token");

        assertTrue(existing.getRevoked());
        verify(refreshTokenRepository).save(existing);
    }

    @Test
    void revoke_withUnknownToken_isNoOp() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> refreshTokenService.revoke("unknown-raw-token"));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}
