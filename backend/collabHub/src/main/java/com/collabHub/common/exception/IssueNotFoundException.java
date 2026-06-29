package com.collabHub.common.exception;

/**
 * Exception thrown when an issue is not found by ID or key.
 * Mirrors ChannelNotFoundException — handled in GlobalExceptionHandler.
 */
public class IssueNotFoundException extends RuntimeException {

    public IssueNotFoundException(String message) {
        super(message);
    }

    public IssueNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
