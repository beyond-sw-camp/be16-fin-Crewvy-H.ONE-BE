package com.crewvy.common.exception;

/**
 * 날짜 범위가 올바르지 않을 때 발생하는 예외
 */
public class InvalidDateRangeException extends BusinessException {

    public InvalidDateRangeException(String message) {
        super(message);
    }

    public InvalidDateRangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
