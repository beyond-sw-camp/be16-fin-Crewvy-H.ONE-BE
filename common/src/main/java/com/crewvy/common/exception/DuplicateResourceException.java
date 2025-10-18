package com.crewvy.common.exception;

/**
 * 리소스가 이미 존재할 때 발생하는 예외
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
