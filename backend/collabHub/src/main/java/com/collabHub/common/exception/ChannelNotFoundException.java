package com.collabHub.common.exception;

/**
 * Exception thrown when user is not found
 */
public class ChannelNotFoundException extends RuntimeException {
    
    public ChannelNotFoundException(String message) {
        super(message);
    }

    public ChannelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
