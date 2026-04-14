package com.collabHub.common.exception;

public class UserAccessDeniedException extends RuntimeException {
    public UserAccessDeniedException(String message) {
        super(message);
    }

    public UserAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
