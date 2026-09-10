package com.joseph.sensitivewordsservice.security;

import com.joseph.sensitivewordsservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /**
     * Filter method runs once per request.
     * Extracts JWT token from Authorization header and validates it.
     * If valid, sets authentication in SecurityContext.
     * 
     * Header format: Authorization: Bearer <token>
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Extract token from "Authorization: Bearer <token>" header
            String token = extractToken(request);

            // If token exists and is valid, authenticate the request
            if (token != null && jwtService.validateToken(token)) {
                String username = jwtService.extractUsername(token);
                
                // Create authentication token - no credentials needed (JWT is stateless)
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        username, null, new ArrayList<>()
                );
                
                // Set authentication in SecurityContext - available to controllers
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.debug("JWT token validated for user: {}", username);
            }
        } catch (Exception e) {
            // Don't block request on filter errors - let SecurityFilterChain handle 401
            log.error("JWT filter error: {}", e.getMessage());
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from Authorization header.
     * Expected format: "Bearer <token>"
     * 
     * @param request HTTP request
     * @return JWT token string, or null if header not present or invalid format
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
