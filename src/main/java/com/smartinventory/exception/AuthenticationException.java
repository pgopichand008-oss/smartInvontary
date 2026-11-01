package com.smartinventory.exception;

/**
 * Thrown when user authentication fails (e.g., bad credentials or disabled account).
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
