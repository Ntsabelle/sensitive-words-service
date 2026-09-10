package com.joseph.sensitivewordsservice.repository;

import com.joseph.sensitivewordsservice.entity.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity persistence.
 * Provides database access methods for user authentication and account management.
 * 
 * Caching Strategy:
 * - findByUsername() results cached for 10 minutes (see cache configuration)
 * - Cache invalidated on user update/delete operations
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username.
     * Username is unique, so this returns at most one result.
     * Results are cached to reduce database queries during authentication.
     * 
     * @param username Login username
     * @return Optional containing user if found
     */
    @Cacheable(value = "users", key = "#username", unless = "#result == null")
    Optional<User> findByUsername(String username);
}
