package com.collabHub.common.exception;

/**
 * Exception thrown when a project is not found.
 * Mirrors ChannelNotFoundException — handled in GlobalExceptionHandler.
 */
public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(String message) {
        super(message);
    }

    public ProjectNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
