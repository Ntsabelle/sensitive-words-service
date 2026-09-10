package com.joseph.sensitivewordsservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    // Secret key for HMAC signing - should be at least 256 bits in production
    @Value("${jwt.secret:my-secret-key-for-jwt-token-generation-min-256-bits}")
    private String secretKey;

    // Token expiration time in milliseconds
    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    /**
     * Get SecretKey for JWT signing and verification.
     * Uses HMAC SHA-256 algorithm.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * Generate a new JWT token for the given username.
     * Token includes issued-at time and expiration time.
     * 
     * @param username The username to encode in the token
     * @return Signed JWT token string
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        log.info("Generating token for user: {}, expires at: {}", username, expiryDate);
        
        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract username from JWT token.
     * Validates signature and expiration.
     * 
     * @param token JWT token string
     * @return Username encoded in the token
     * @throws IllegalArgumentException if token is invalid or expired
     */
    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired");
            throw new IllegalArgumentException("Token has expired", e);
        } catch (JwtException e) {
            log.error("JWT extraction failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    /**
     * Validate JWT token signature and expiration.
     * Explicit expiration check ensures tokens expire even with clock skew.
     * 
     * @param token JWT token string
     * @return true if token is valid and not expired, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            // Explicit expiration check - don't rely on parser alone
            Date expiryTime = claims.getExpiration();
            Date now = new Date();
            
            if (expiryTime.before(now)) {
                log.warn("Token expired. Expiry: {}, Now: {}", expiryTime, now);
                return false;
            }
            
            log.debug("Token valid for user: {}", claims.getSubject());
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}


