package com.joseph.sensitivewordsservice.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed distributed rate limiting configuration.
 * When Redis is available, uses it for synchronizing rate limits across multiple instances.
 * Falls back to in-memory buckets when Redis is not configured.
 */
@Configuration
@Slf4j
public class RedisRateLimitConfig {

    /**
     * Redis-based rate limiting service using StringRedisTemplate.
     * Synchronizes bucket state across multiple JVM instances.
     */
    @Bean
    @ConditionalOnProperty(name = "spring.redis.host")
    public RateLimitingService redisRateLimitingService(StringRedisTemplate redisTemplate) {
        log.info("Initializing Redis-backed distributed rate limiting");
        return new RedisRateLimitingService(redisTemplate);
    }

    /**
     * Service interface for rate limiting - abstracts in-memory vs Redis implementation.
     */
    public interface RateLimitingService {
        /**
         * Check if a request should be allowed under rate limit.
         * @param bucketName Name of the rate limit bucket (e.g., "sanitize", "login")
         * @param key Unique identifier for the client/user (typically IP address)
         * @return true if request should be allowed, false if rate limit exceeded
         */
        boolean allowRequest(String bucketName, String key);
    }

    /**
     * Redis-backed rate limiting using atomic counters.
     * Each bucket:key pair has a counter and expiration.
     * Distributed across instances via Redis.
     */
    @Slf4j
    public static class RedisRateLimitingService implements RateLimitingService {
        private final StringRedisTemplate redisTemplate;
        
        // Rate limit config: bucket name -> (requests per minute)
        private static final int SANITIZE_LIMIT = 100;
        private static final int LOGIN_LIMIT = 5;
        private static final int WORD_MGMT_LIMIT = 20;
        private static final int WINDOW_MINUTES = 1;

        public RedisRateLimitingService(StringRedisTemplate redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        @Override
        public boolean allowRequest(String bucketName, String key) {
            try {
                String redisKey = "rate-limit:" + bucketName + ":" + key;
                int limit = getBucketLimit(bucketName);
                long ttlSeconds = WINDOW_MINUTES * 60;
                
                Long count = redisTemplate.opsForValue().increment(redisKey);
                
                if (count == 1) {
                    // First request in this window, set expiration
                    redisTemplate.expire(redisKey, ttlSeconds, TimeUnit.SECONDS);
                }
                
                return count <= limit;
            } catch (Exception e) {
                log.error("Redis rate limiting check failed for {}/{}, allowing request", bucketName, key, e);
                return true; // Fail open to prevent service disruption
            }
        }
        
        private int getBucketLimit(String bucketName) {
            return switch (bucketName) {
                case "sanitize" -> SANITIZE_LIMIT;
                case "login" -> LOGIN_LIMIT;
                case "wordMgmt" -> WORD_MGMT_LIMIT;
                default -> 100;
            };
        }
    }
}

