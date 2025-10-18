package com.crewvy.common.exception;

/**
 * 인증(GPS, IP 등) 실패 시 발생하는 예외
 */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
