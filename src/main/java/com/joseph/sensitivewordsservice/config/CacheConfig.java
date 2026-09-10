package com.joseph.sensitivewordsservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for user authentication and frequently accessed data.
 * 
 * Uses Caffeine cache (high-performance in-memory cache):
 * - User cache: 10-minute TTL for findByUsername() results
 * - Maximum 1000 entries to prevent unbounded memory growth
 * - Automatic eviction on access or time-based expiry
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager with appropriate TTL and limits.
     * Users cache entries expire after 10 minutes of write.
     * 
     * @return CaffeineCacheManager with "users" cache configured
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("users");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats());
        return cacheManager;
    }
}
