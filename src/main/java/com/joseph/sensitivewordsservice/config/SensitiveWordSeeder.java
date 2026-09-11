package com.joseph.sensitivewordsservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joseph.sensitivewordsservice.entity.SensitiveWord;
import com.joseph.sensitivewordsservice.repository.SensitiveWordRepository;
import com.joseph.sensitivewordsservice.service.SanitizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

/**
 * Preloads the sensitive_words table with the company's SQL-keyword sensitive
 * word list on application startup.
 * <p>
 * Runs once, only when the table is empty, so it is safe on every restart:
 * - Dev (H2, ddl-auto=create): table is recreated empty each boot -> reseeds every time.
 * - Prod (MSSQL, ddl-auto=validate): table persists -> seeding only happens once, on
 *   first ever startup against a fresh database, and is a no-op afterwards.
 * <p>
 * Source list: src/main/resources/seed-sensitive-words.json
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SensitiveWordSeeder implements ApplicationRunner {

    private static final String SEED_RESOURCE = "classpath:seed-sensitive-words.json";

    private final SensitiveWordRepository sensitiveWordRepository;
    private final SanitizationService sanitizationService;
    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourceResolver;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (sensitiveWordRepository.count() > 0) {
            log.info("Sensitive words table already populated ({} word(s)) - skipping seed",
                    sensitiveWordRepository.count());
            return;
        }

        List<String> words = loadWordList();
        if (words.isEmpty()) {
            log.warn("Seed word list at {} was empty - nothing preloaded", SEED_RESOURCE);
            return;
        }

        Instant now = Instant.now();
        List<SensitiveWord> entities = words.stream()
                .map(String::trim)
                .filter(w -> !w.isBlank())
                .distinct()
                .map(word -> SensitiveWord.builder()
                        .word(word)
                        .active(true)
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();

        sensitiveWordRepository.saveAll(entities);
        sanitizationService.refresh();
        log.info("Preloaded {} sensitive word(s) from {}", entities.size(), SEED_RESOURCE);
    }

    private List<String> loadWordList() throws Exception {
        Resource resource = resourceResolver.getResource(SEED_RESOURCE);
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readValue(in, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        }
    }
}
