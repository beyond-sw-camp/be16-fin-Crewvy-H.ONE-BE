package com.crewvy.common.exception;

/**
 * 미승인 디바이스로 접근 시도 시 발생하는 예외
 */
public class UnauthorizedDeviceException extends RuntimeException {
    public UnauthorizedDeviceException(String message) {
        super(message);
    }
}
