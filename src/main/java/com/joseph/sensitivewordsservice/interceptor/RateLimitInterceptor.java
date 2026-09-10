package com.joseph.sensitivewordsservice.interceptor;

import com.joseph.sensitivewordsservice.annotation.RateLimit;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to enforce rate limiting on endpoints marked with @RateLimit annotation.
 * 
 * Returns 429 Too Many Requests if rate limit exceeded.
 */
@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;
        RateLimit annotation = method.getMethodAnnotation(RateLimit.class);

        if (annotation == null) {
            return true;
        }

        String bucketName = annotation.bucketName();
        Bucket bucket = applicationContext.getBean(bucketName + "Bucket", Bucket.class);

        if (bucket == null) {
            log.warn("Rate limit bucket not found: {}", bucketName);
            return true;
        }

        String clientIp = getClientIp(request);
        if (bucket.tryConsume(1)) {
            return true;
        }

        log.warn("Rate limit exceeded for {} from {}", bucketName, clientIp);
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Too many requests - rate limit exceeded\"}");
        return false;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}

