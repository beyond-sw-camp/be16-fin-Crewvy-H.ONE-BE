package com.crewvy.common.exception;

// JSON 직렬화 실패 시 발생
public class SerializationException extends RuntimeException {
    public SerializationException(String message) {
        super(message);
    }
}
