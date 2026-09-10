package com.joseph.sensitivewordsservice.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Rate limiting configuration using Bucket4j.
 * 
 * Defines rate limits for expensive operations:
 * - Sanitize: 100 requests per minute per user
 * - Login: 5 attempts per minute (protects against brute force)
 * - Sensitive word management: 20 operations per minute
 */
@Configuration
public class RateLimitConfig {

    /**
     * Create bucket for sanitize endpoint.
     * Allows 100 requests per minute.
     */
    @Bean
    public Bucket sanitizeBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create bucket for login endpoint.
     * Allows only 5 attempts per minute to prevent brute force attacks.
     */
    @Bean
    public Bucket loginBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Create bucket for sensitive word management (create, update, delete).
     * Allows 20 operations per minute.
     */
    @Bean
    public Bucket sensitiveWordBucket() {
        Bandwidth limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)));
        return Bucket4j.builder()
                .addLimit(limit)
                .build();
    }
}
