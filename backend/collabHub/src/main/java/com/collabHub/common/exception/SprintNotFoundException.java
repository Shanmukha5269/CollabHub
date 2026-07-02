package com.collabHub.common.exception;

/**
 * Exception thrown when a sprint is not found.
 * Mirrors ChannelNotFoundException — handled in GlobalExceptionHandler.
 */
public class SprintNotFoundException extends RuntimeException {

    public SprintNotFoundException(String message) {
        super(message);
    }

    public SprintNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
