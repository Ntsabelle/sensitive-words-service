package com.joseph.sensitivewordsservice.service;

import com.joseph.sensitivewordsservice.repository.SensitiveWordRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SanitizationServiceTest {

    @Mock
    private SensitiveWordRepository sensitiveWordRepository;

    private MeterRegistry meterRegistry;

    private SanitizationService sanitizationService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        
        sanitizationService = new SanitizationService(sensitiveWordRepository, meterRegistry);
        sanitizationService.init();
    }

    @Test
    void testSanitizeWithMatches() {
        when(sensitiveWordRepository.findAllActiveWords()).thenReturn(Arrays.asList("badword", "offensive"));

        sanitizationService.refresh();

        SanitizationService.SanitizeResult result = sanitizationService.sanitize("This is a badword text");
        
        assertNotNull(result);
        assertTrue(result.matchCount() > 0);
        assertTrue(result.sanitizedText().contains("*"));
    }

    @Test
    void testSanitizeWithNoMatches() {
        when(sensitiveWordRepository.findAllActiveWords()).thenReturn(Arrays.asList("badword", "offensive"));

        sanitizationService.refresh();

        SanitizationService.SanitizeResult result = sanitizationService.sanitize("This is clean text");
        
        assertNotNull(result);
        assertEquals(0, result.matchCount());
        assertEquals("This is clean text", result.sanitizedText());
    }

    @Test
    void testSanitizeEmptyInput() {
        SanitizationService.SanitizeResult result = sanitizationService.sanitize("");
        
        assertNotNull(result);
        assertEquals(0, result.matchCount());
        assertEquals("", result.sanitizedText());
    }

    @Test
    void testSanitizeNullInput() {
        SanitizationService.SanitizeResult result = sanitizationService.sanitize(null);
        
        assertNotNull(result);
        assertEquals(0, result.matchCount());
        assertNull(result.sanitizedText());
    }

    @Test
    void testSanitizeMultipleMatches() {
        when(sensitiveWordRepository.findAllActiveWords()).thenReturn(List.of("bad"));

        sanitizationService.refresh();

        SanitizationService.SanitizeResult result = sanitizationService.sanitize("bad bad bad");
        
        assertNotNull(result);
        assertEquals(3, result.matchCount());
        assertTrue(result.sanitizedText().contains("***"));
    }

    @Test
    void testSanitizeCaseInsensitive() {
        when(sensitiveWordRepository.findAllActiveWords()).thenReturn(List.of("badword"));

        sanitizationService.refresh();

        SanitizationService.SanitizeResult result = sanitizationService.sanitize("BADWORD badword BaDwOrD");
        
        assertNotNull(result);
        assertEquals(3, result.matchCount());
    }

    @Test
    void testRefreshWithEmptyWords() {
        when(sensitiveWordRepository.findAllActiveWords()).thenReturn(List.of());

        sanitizationService.refresh();

        SanitizationService.SanitizeResult result = sanitizationService.sanitize("Some text");
        
        assertNotNull(result);
        assertEquals(0, result.matchCount());
        assertEquals("Some text", result.sanitizedText());
    }

    @Test
    void testSanitizeWithWordBoundaries() {
        when(sensitiveWordRepository.findAllActiveWords()).thenReturn(List.of("bad"));

        sanitizationService.refresh();

        SanitizationService.SanitizeResult result = sanitizationService.sanitize("badness bad");
        
        assertNotNull(result);
        assertEquals(1, result.matchCount());
        assertTrue(result.sanitizedText().contains("badness"));
    }

    @Test
    void testSanitizePreservesNonMatchedText() {
        when(sensitiveWordRepository.findAllActiveWords()).thenReturn(List.of("bad"));

        sanitizationService.refresh();

        String input = "This bad text should have partial censoring";
        SanitizationService.SanitizeResult result = sanitizationService.sanitize(input);
        
        assertNotNull(result);
        assertTrue(result.sanitizedText().contains("This"));
        assertTrue(result.sanitizedText().contains("text"));
        assertTrue(result.sanitizedText().contains("should"));
    }
}
