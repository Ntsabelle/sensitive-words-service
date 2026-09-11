package com.joseph.sensitivewordsservice.service;


import com.joseph.sensitivewordsservice.repository.SensitiveWordRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SanitizationService {

    // Pattern that never matches - used as default to avoid null checks
    private static final Pattern NEVER_MATCHES = Pattern.compile("(?!)");

    private final SensitiveWordRepository sensitiveWordRepository;
    private final MeterRegistry meterRegistry;

    // Thread-safe holder for compiled regex pattern
    private final AtomicReference<Pattern> compiledPattern = new AtomicReference<>(NEVER_MATCHES);

    // ReadWriteLock for pattern refresh - allows multiple concurrent readers during sanitize,
    // exclusive access during refresh. Better performance than ReentrantLock for read-heavy workloads.
    private final ReadWriteLock refreshLock = new ReentrantReadWriteLock();

    // Cache hash to avoid unnecessary pattern recompilation
    private volatile int cachedWordListHash = 0;

    // Metrics: track sanitization performance
    private Timer sanitizeTimer;
    private Counter matchCounter;

    /**
     * Initialize metrics on service startup.
     * Loads sensitive words from database and compiles regex pattern.
     */
    @PostConstruct
    public void init() {
        // Timer tracks how long sanitization takes
        sanitizeTimer = Timer.builder("sensitive.words.sanitization.duration")
                .description("Time taken to scan and mask sensitive words from input for sanitization service")
                .register(meterRegistry);
        // Counter tracks total number of sensitive words matched
        matchCounter = Counter.builder("sanitize.words.match.total")
                .description("Cumulative count of sensitive words matched and sanitized from input for sanitization service")
                .register(meterRegistry);
        refresh();
    }

    @Transactional(readOnly = true)
    public void refresh() {
        // Acquire write lock for pattern rebuild
        refreshLock.writeLock().lock();
        try{
            List<String> activeWords = sensitiveWordRepository.findAllActiveWords();

            List<String> sorted = activeWords.stream()
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .collect(Collectors.toList());

            int newHash = sorted.hashCode();
            if (newHash == cachedWordListHash) {
                log.debug("Sensitive word cache unchanged ({} word(s)), skipping refresh", sorted.size());
                return;
            }
            cachedWordListHash = newHash;

            if(sorted.isEmpty()){
                compiledPattern.set(NEVER_MATCHES);
                log.info("Sensitive word cache refreshed - no active words", sorted.size());
                return;
            }

            String alternation = sorted.stream()
                    .map(Pattern::quote)
                    .collect(Collectors.joining("|"));

            Pattern pattern = Pattern.compile("\\b(?:" + alternation + ")\\b", Pattern.CASE_INSENSITIVE);
            compiledPattern.set(pattern);
            log.info("Sensitive word cache refreshed ({} word(s))", sorted.size());
        } finally {
            refreshLock.writeLock().unlock();
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSensitiveWordChanged(SensitiveWordChangedEvent event) {
        log.debug("Sensitive word changed event received, refreshing cache");
        refresh();
    }

    public SanitizeResult sanitize(String input) {
        long startNanos = System.nanoTime();
        SanitizeResult result = doSanitize(input);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        sanitizeTimer.record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        if(result.matchCount() > 0){
            matchCounter.increment(result.matchCount());
        }
        log.debug("Sanitize complete: input length: {}, matchCount: {}, durationMs: {}",
                input == null ? 0 : input.length(), result.matchCount(), elapsedMs);
        return result;
    }

    private SanitizeResult doSanitize(String input) {
        if (input == null || input.isEmpty()) {
            return new SanitizeResult(input, 0);
        }

        // Acquire read lock for pattern access - allows multiple concurrent sanitizations
        refreshLock.readLock().lock();
        try {
            Matcher matcher = compiledPattern.get().matcher(input);
            StringBuilder sb = new StringBuilder(input.length());
            int matchCount = 0;
            int lastMatchEnd = 0;

            while(matcher.find()){
                sb.append(input, lastMatchEnd, matcher.start());
                sb.append("*".repeat(matcher.end() - matcher.start()));
                lastMatchEnd = matcher.end();
                matchCount++;
            }
            sb.append(input, lastMatchEnd, input.length());
            return new SanitizeResult(sb.toString(), matchCount);
        } finally {
            refreshLock.readLock().unlock();
        }
    }

    public record SanitizeResult(String sanitizedText, int matchCount) {}


}
