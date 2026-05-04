package com.collabHub.common.exception;

/**
 * Exception thrown when a user attempts to access a suspended workspace
 */
public class WorkspaceSuspendedException extends RuntimeException {
    
    public WorkspaceSuspendedException(String message) {
        super(message);
    }
    
    public WorkspaceSuspendedException(String message, Throwable cause) {
        super(message, cause);
    }
}
