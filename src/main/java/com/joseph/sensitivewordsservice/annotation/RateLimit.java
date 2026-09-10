package com.joseph.sensitivewordsservice.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply rate limiting to controller methods.
 * 
 * Usage: @RateLimit(bucketName = "sanitize")
 * 
 * Supported bucket names:
 * - sanitize: 100 req/min
 * - login: 5 req/min
 * - sensitiveWord: 20 req/min
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    String bucketName();
}
