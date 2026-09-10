package com.joseph.sensitivewordsservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    /**
     * Password encoder bean - uses BCrypt with default strength 10.
     * Automatically invoked for password hashing in AuthService.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configure Spring Security filter chain.
     * 
     * Security rules:
     * - CSRF disabled (we use stateless JWT auth)
     * - Session management: STATELESS (no server-side sessions)
     * - Public endpoints: /auth/**, swagger, health check, h2-console
     * - Protected endpoints: all /api/v1/** require JWT token
     * - JWT filter runs before UsernamePasswordAuthenticationFilter
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF - not needed for stateless JWT API
                .csrf().disable()
                
                // Use stateless session - no jsessionid cookies
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                
                // Configure authorization rules
                .authorizeHttpRequests(authorize -> authorize
                        // Auth endpoints are public (registration, login)
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        
                        // Swagger documentation is public
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        
                        // H2 console is public (dev only - disable in production)
                        .requestMatchers("/h2-console/**").permitAll()
                        
                        // Health and info endpoints are public
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                
                // Allow H2 console frames (only for dev)
                .headers().frameOptions().disable()
                .and()
                
                // Add custom JWT filter before standard authentication filter
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
