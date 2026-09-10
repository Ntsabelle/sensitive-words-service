package com.joseph.sensitivewordsservice.repository;

import com.joseph.sensitivewordsservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity persistence.
 * Provides database access methods for user authentication and account management.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username.
     * Username is unique, so this returns at most one result.
     * 
     * @param username Login username
     * @return Optional containing user if found
     */
    Optional<User> findByUsername(String username);
}
