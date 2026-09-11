package com.joseph.sensitivewordsservice.service;

import org.springframework.context.ApplicationEvent;

/**
 * Event published when a sensitive word is changed (created, updated, or deleted).
 * Triggers cache refresh in SanitizationService after transaction commits.
 */
public class SensitiveWordChangedEvent extends ApplicationEvent {
    private final Long wordId;

    public SensitiveWordChangedEvent(Object source, Long wordId) {
        super(source);
        this.wordId = wordId;
    }

    public SensitiveWordChangedEvent(Long wordId) {
        super(wordId);
        this.wordId = wordId;
    }

    public Long getWordId() {
        return wordId;
    }
}
