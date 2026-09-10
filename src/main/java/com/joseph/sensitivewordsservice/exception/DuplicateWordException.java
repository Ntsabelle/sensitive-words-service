package com.joseph.sensitivewordsservice.exception;

public class DuplicateWordException extends RuntimeException {
    public DuplicateWordException(String message) {
        super(message);
    }
}
